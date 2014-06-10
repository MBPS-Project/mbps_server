package ch.uzh.csg.mbps.server.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.minidev.json.parser.ParseException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.keys.CustomKeyPair;
import ch.uzh.csg.mbps.server.security.KeyHandler;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;

/**
 * {@link Initializer} is taking care of starting necessary tasks after
 * successfully starting up Tomcat server application.
 */
public class Initializer implements InitializingBean{
	private static Logger LOGGER = Logger.getLogger(Initializer.class);
	
	private static final String KEY_FILE_NAME = "ServerKeys.xml";

	@SuppressWarnings("resource")
	public void afterPropertiesSet() {
		try {
			BitcoindController.backupWallet();
			//activates receivePayIn/Out Listener
			BitcoindController.listenIncomingTransactions();
			BitcoindController.listenOutgoingTransactions();
			//activates Task for checking PayOutRules
			new ClassPathXmlApplicationContext("HourlyQuartz.xml");
			
			deleteOldKeysFile();
			
			File serverKeys = null;
			URI uri = null;
			try {
				uri = getClass().getResource(File.separator + KEY_FILE_NAME).toURI();
			} catch (URISyntaxException e) {
				LOGGER.info("Couldnt create server keys", e);
			}
			
			serverKeys = new File(uri);
			
			try {
				if (!serverKeys.exists()) {
					boolean created = serverKeys.createNewFile();
					if (!created)
						throw new Exception("could not create file "+uri);
					
					KeyPair keyPair = KeyHandler.generateKeyPair();
					CustomKeyPair ckp = new CustomKeyPair(PKIAlgorithm.DEFAULT.getCode(), (byte) 1, KeyHandler.encodePublicKey(keyPair.getPublic()), KeyHandler.encodePrivateKey(keyPair.getPrivate()));
					saveToXml(serverKeys, ckp);
					
					Constants.PRIVATEKEY = keyPair.getPrivate();
					Constants.PUBLICKEY = keyPair.getPublic();	
				} else {
					CustomKeyPair ckp = loadFromXml(serverKeys, (byte) 1);
					
					Constants.PRIVATEKEY = KeyHandler.decodePrivateKey(ckp.getPrivateKey());
					Constants.PUBLICKEY = KeyHandler.decodePublicKey(ckp.getPublicKey());
				}
			} catch (Exception e) {
				LOGGER.error("Problem reading Serverkeys from Input File", e);
			}
		} catch (BitcoinException e) {
			LOGGER.error("Bitcoind Exception: Couldn't initialize receivment of Bitcoin PayIN Transactions");
		}

		try {
			ExchangeRates.updateExchangeRateUsdChf();
		} catch (ParseException | IOException e) {
			LOGGER.error("Problem updating USD/CHF exchange rate.");
		}

		updateExchangeRateTask();
	}

	private void deleteOldKeysFile() {
		try {
			String keyFilePath = "ServerKeys.txt";
			File f = new File(getClass().getResource("/" + keyFilePath).toURI());
			if (f.exists()) {
				f.delete();
			}
		} catch (URISyntaxException e) {
		}
	}
	
	private void saveToXml(File serverKeys, CustomKeyPair ckp) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
 
		// root element
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("customkeypairs");
		doc.appendChild(rootElement);
		
		// custom key pair element
		Element customKeyPair = doc.createElement("customkeypair");
		rootElement.appendChild(customKeyPair);
		
		// pki algorithm element
		Element pkiAlgorithm = doc.createElement("pkialgorithm");
		pkiAlgorithm.appendChild(doc.createTextNode(Byte.toString(ckp.getPkiAlgorithm())));
		rootElement.appendChild(pkiAlgorithm);
 
		// key number element
		Element keyNumber = doc.createElement("keynumber");
		keyNumber.appendChild(doc.createTextNode(Byte.toString(ckp.getKeyNumber())));
		customKeyPair.appendChild(keyNumber);
 
		// public key element
		Element publicKey = doc.createElement("publickey");
		publicKey.appendChild(doc.createTextNode(ckp.getPublicKey()));
		customKeyPair.appendChild(publicKey);
 
		// private key element
		Element privateKey = doc.createElement("privatekey");
		privateKey.appendChild(doc.createTextNode(ckp.getPrivateKey()));
		customKeyPair.appendChild(privateKey);
 
		// write the content into xml file
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(serverKeys);
 
		transformer.transform(source, result);
	}

	private CustomKeyPair loadFromXml(File serverKeys, byte keyNumber) throws Exception {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(serverKeys);
		
		doc.getDocumentElement().normalize();
		
		CustomKeyPair ckp = null;
		
		NodeList nList = doc.getElementsByTagName("customkeypairs");
		for (int i=0; i<nList.getLength(); i++) {
			if ((i+1) == (keyNumber & 0xFF)) {
				Node n = nList.item(i);
				
				Element e = (Element) n;
				
				String pkiAlgorithmString = e.getElementsByTagName("pkialgorithm").item(0).getTextContent();
				byte pkiAlgorithm = Byte.parseByte(pkiAlgorithmString);
				String keyNumberString = e.getElementsByTagName("keynumber").item(0).getTextContent();
				byte keyNr = Byte.parseByte(keyNumberString);
				String publicKey = e.getElementsByTagName("publickey").item(0).getTextContent();
				String privateKey = e.getElementsByTagName("privatekey").item(0).getTextContent();
				
				ckp = new CustomKeyPair(pkiAlgorithm, keyNr, publicKey, privateKey);
				break;
			}
		}
		
		return ckp;
	}

	private static void updateExchangeRateTask(){
		try {
			//update ExchangeRate every 5 seconds
			ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
			exec.scheduleAtFixedRate(new Runnable() {
			  @Override
			  public void run() {
			   	try {
					ExchangeRates.update();
				} catch (Throwable t) {
					LOGGER.error("Problem updating exchangerate. " + t.getMessage());
				}
			  }
			}, 30, Config.EXCHANGE_RATE_UPDATE_TIME, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOGGER.error("Problem updating exchangerate. " + e.getMessage());
		}
	}
	
}
