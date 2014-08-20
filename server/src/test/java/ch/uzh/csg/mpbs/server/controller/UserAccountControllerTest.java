package ch.uzh.csg.mpbs.server.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.KeyPair;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.hibernate.HibernateException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import ch.uzh.csg.mbps.customserialization.Currency;
import ch.uzh.csg.mbps.customserialization.DecoderFactory;
import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;
import ch.uzh.csg.mbps.customserialization.PaymentRequest;
import ch.uzh.csg.mbps.customserialization.ServerPaymentRequest;
import ch.uzh.csg.mbps.customserialization.ServerPaymentResponse;
import ch.uzh.csg.mbps.customserialization.ServerResponseStatus;
import ch.uzh.csg.mbps.keys.CustomPublicKey;
import ch.uzh.csg.mbps.responseobject.CreateTransactionTransferObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject;
import ch.uzh.csg.mbps.responseobject.CustomResponseObject.Type;
import ch.uzh.csg.mbps.responseobject.GetHistoryTransferObject;
import ch.uzh.csg.mbps.responseobject.ReadAccountTransferObject;
import ch.uzh.csg.mbps.server.clientinterface.IUserAccount;
import ch.uzh.csg.mbps.server.domain.ResetPassword;
import ch.uzh.csg.mbps.server.domain.UserAccount;
import ch.uzh.csg.mbps.server.security.KeyHandler;
import ch.uzh.csg.mbps.server.service.UserAccountService;
import ch.uzh.csg.mbps.server.util.exceptions.EmailAlreadyExistsException;
import ch.uzh.csg.mbps.server.util.exceptions.InvalidEmailException;
import ch.uzh.csg.mbps.server.util.exceptions.InvalidUsernameException;
import ch.uzh.csg.mbps.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.mbps.server.util.exceptions.UsernameAlreadyExistsException;
import ch.uzh.csg.mbps.util.Converter;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;
import com.fasterxml.jackson.databind.ObjectMapper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:context.xml",
		"classpath:test-database.xml",
		"classpath:view.xml",
		"classpath:security.xml"})
@WebAppConfiguration
public class UserAccountControllerTest {

	@Autowired
	private WebApplicationContext webAppContext;

	@Autowired
	private FilterChainProxy springSecurityFilterChain;
	
	@Autowired
	private IUserAccount userAccountService;

	private static MockMvc mockMvc;

	private static boolean initialized = false;
	private static UserAccount test22;
	private static UserAccount test23;
	private static UserAccount test23_2;
	private static UserAccount test24;
	private static UserAccount test25;
	private static UserAccount test26;
	private static UserAccount test26_1;
	private static UserAccount test27;
	private static UserAccount test29;
	private static UserAccount test30;
	private static UserAccount test31;
	private static UserAccount test32;
	private static UserAccount test33;

	@Before
	public void setUp() {
		UserAccountService.enableTestingMode();

		if (!initialized) {
			mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext).addFilter(springSecurityFilterChain).build();

			test22 = new UserAccount("test22", "chuck22@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test23 = new UserAccount("test23", "chuck23@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test23_2 = new UserAccount("test23", "chuck23@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test24 = new UserAccount("test24", "chuck24@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test25 = new UserAccount("test25", "chuck25@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test26 = new UserAccount("test26", "chuck26@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test26_1 = new UserAccount("test26_1", "chuck261@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test27 = new UserAccount("test27", null, "i-don't-need-one");
			test29 = new UserAccount("test29", "chuck29@bitcoin.csg.uzh.ch", "i-don't-need-one");
			test30 = new UserAccount("test30", "dandeliox@gmail.com", "i-don't-need-one");
			test31 = new UserAccount("test31", "test31@gmail.com", "i-don't-need-one");
			test32 = new UserAccount("test32", "test32@gmail.com", "i-don't-need-one");
			test33 = new UserAccount("test33", "test33@gmail.com", "i-don't-need-one");

			initialized = true;
		}
	}

	@After
	public void tearDown() {
		UserAccountService.disableTestingMode();
	}

	@Test
	public void testCreateUserAccount() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String asString = mapper.writeValueAsString(test22);

		MvcResult mvcResult = mockMvc.perform(post("/user/create").secure(false).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk()).andReturn();

		String contentAsString = mvcResult.getResponse().getContentAsString();
		CustomResponseObject receivedObject = mapper.readValue(contentAsString, CustomResponseObject.class);

		assertNotNull(receivedObject);
		assertEquals(true, receivedObject.isSuccessful());
	}

	@Test
	public void testCreateUserAccount_UsernameNotUnique() throws Exception {
		assertTrue(userAccountService.createAccount(test23));

		ObjectMapper mapper = new ObjectMapper();
		String asString = mapper.writeValueAsString(test23_2);

		//this will fail since the username is not unique
		MvcResult mvcResult = mockMvc.perform(post("/user/create").secure(false).contentType(MediaType.APPLICATION_JSON).content(asString)).andReturn();

		String contentAsString = mvcResult.getResponse().getContentAsString();
		CustomResponseObject receivedObject = mapper.readValue(contentAsString, CustomResponseObject.class);

		assertNotNull(receivedObject);
		assertEquals(false, receivedObject.isSuccessful());
		assertEquals("Username already exists.", receivedObject.getMessage());
	}

	@Test(expected = InvalidEmailException.class)  
	public void testCreateUserAccount_EmptyFields() throws HibernateException, UsernameAlreadyExistsException, BitcoinException, InvalidUsernameException, InvalidEmailException, EmailAlreadyExistsException {
		userAccountService.createAccount(test27);
	}

	@Test
	public void testVerifyEmail() throws Exception {
		String plainTextPassword = test29.getPassword();
		assertTrue(userAccountService.createAccount(test29));

		UserAccount fromDB = userAccountService.getByUsername(test29.getUsername());
		String token = userAccountService.getTokenForUser(fromDB.getId());

		MvcResult mvcResult = mockMvc.perform(get("/user/verify/"+token).secure(false))
				.andExpect(status().isOk())
				.andReturn();

		mvcResult = mockMvc.perform(get("/user/verify/"+token).secure(false))
				.andExpect(status().isOk()).andExpect(view().name("FailedEmailVerification"))
				.andReturn();

		HttpSession session = loginAndGetSession(test29.getUsername(), plainTextPassword);

		mvcResult = mockMvc.perform(get("/user/afterLogin").secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
				.andReturn();

		String asString = mvcResult.getResponse().getContentAsString();

		ObjectMapper mapper = new ObjectMapper();
		CustomResponseObject transferObject = mapper.readValue(asString, CustomResponseObject.class);

		assertEquals(true, transferObject.isSuccessful());
		assertNotNull(transferObject.getServerPublicKey());
		assertNotNull(transferObject.getReadAccountTO());

		ReadAccountTransferObject readAccountTO = transferObject.getReadAccountTO();

		assertEquals(test29.getUsername(), readAccountTO.getUserAccount().getUsername());
		assertEquals(test29.getEmail(), readAccountTO.getUserAccount().getEmail());

		logout(mvcResult);
	}

	@Test
	public void testReadUserAccount_FailNotAuthorized() throws Exception {
		mockMvc.perform(get("/user/afterLogin").secure(false)).andExpect(status().isUnauthorized());
	}

	@Test
	public void testReadUserAccount() throws Exception {
		String plainTextPassword = test24.getPassword();
		createAccountAndVerifyAndReload(test24, new BigDecimal(0.0));
		HttpSession session = loginAndGetSession(test24.getUsername(), plainTextPassword);

		MvcResult mvcResult = mockMvc.perform(get("/user/afterLogin").secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
				.andReturn();

		String asString = mvcResult.getResponse().getContentAsString();

		ObjectMapper mapper = new ObjectMapper();
		CustomResponseObject transferObject = mapper.readValue(asString, CustomResponseObject.class);

		assertEquals(true, transferObject.isSuccessful());
		assertNotNull(transferObject.getServerPublicKey());
		assertNotNull(transferObject.getReadAccountTO());

		ReadAccountTransferObject readAccountTO = transferObject.getReadAccountTO();

		assertEquals(test24.getUsername(), readAccountTO.getUserAccount().getUsername());
		assertEquals(test24.getEmail(), readAccountTO.getUserAccount().getEmail());

		logout(mvcResult);
	}

	@Test
	public void testUpdateUserAccount_FailNotAuthorized() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		String asString = mapper.writeValueAsString(new UserAccount("test99", "email99", "password99"));

		mockMvc.perform(post("/user/update").secure(false).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testUpdateUserAccount() throws Exception {
		String plainTextPassword = test25.getPassword();
		createAccountAndVerifyAndReload(test25,new BigDecimal(0.0));
		HttpSession session = loginAndGetSession(test25.getUsername(), plainTextPassword);

		String newEmail = "fancy-new-email";
		ObjectMapper mapper = new ObjectMapper();
		String asString = mapper.writeValueAsString(new UserAccount("newname", newEmail, test25.getPassword()));

		mockMvc.perform(post("/user/update").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk());

		MvcResult mvcResult = mockMvc.perform(get("/user/afterLogin").secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
				.andReturn();

		String response = mvcResult.getResponse().getContentAsString();

		mapper = new ObjectMapper();
		CustomResponseObject transferObject = mapper.readValue(response, CustomResponseObject.class);

		assertEquals(true, transferObject.isSuccessful());
		assertNotNull(transferObject.getServerPublicKey());
		assertNotNull(transferObject.getReadAccountTO());

		ReadAccountTransferObject readAccountTO = transferObject.getReadAccountTO();
		assertEquals(test25.getUsername(), readAccountTO.getUserAccount().getUsername());
		assertEquals(newEmail, readAccountTO.getUserAccount().getEmail());

		userAccountService.delete(test25.getUsername());

		mvcResult = mockMvc.perform(post("/user/update").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
				.andExpect(status().isOk())
				.andReturn();

		CustomResponseObject response2 = mapper.readValue(mvcResult.getResponse().getContentAsString(), CustomResponseObject.class);

		assertFalse(response2.isSuccessful());

		logout(mvcResult);
	}

	@Test
	public void testDeleteUserAccountFailNotAuthorized() throws Exception {
		mockMvc.perform(post("/user/update").secure(false).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isUnauthorized());
	}

	@Test
	public void testDeleteUserAccount() throws Exception {
		String plainTextPassword = test26.getPassword();
		createAccountAndVerifyAndReload(test26, new BigDecimal(0.0));
		HttpSession session = loginAndGetSession(test26.getUsername(), plainTextPassword);

		mockMvc.perform(post("/user/delete").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());

		MvcResult mvcResult = mockMvc.perform(get("/user/afterLogin").secure(false).session((MockHttpSession) session)).andReturn();

		String contentAsString = mvcResult.getResponse().getContentAsString();

		ObjectMapper mapper = new ObjectMapper();
		CustomResponseObject cro = mapper.readValue(contentAsString, CustomResponseObject.class);

		assertNotNull(cro);
		assertNull(cro.getReadAccountTO());
		assertNull(cro.getGetHistoryTO());
		assertNull(cro.getCreateTransactionTO());
		assertEquals(false, cro.isSuccessful());
		assertEquals("UserAccount not found.", cro.getMessage());

		mvcResult = mockMvc.perform(post("/user/delete").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		contentAsString = mvcResult.getResponse().getContentAsString();
		cro = mapper.readValue(contentAsString, CustomResponseObject.class);
		assertFalse(cro.isSuccessful());
		assertEquals("UserAccount not found.", cro.getMessage());
	}

	@Test
	public void testDeleteUserAccountFailsBalanceNotZero() throws Exception {
		String plainTextPassword = test26_1.getPassword();
		createAccountAndVerifyAndReload(test26_1, BigDecimal.ONE);
		HttpSession session = loginAndGetSession(test26_1.getUsername(), plainTextPassword);

		MvcResult mvcResult = mockMvc.perform(post("/user/delete").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andReturn();

		String contentAsString = mvcResult.getResponse().getContentAsString();

		ObjectMapper mapper = new ObjectMapper();
		CustomResponseObject cro = mapper.readValue(contentAsString, CustomResponseObject.class);

		assertNotNull(cro);
		assertNull(cro.getReadAccountTO());
		assertNull(cro.getGetHistoryTO());
		assertNull(cro.getCreateTransactionTO());
		assertFalse(cro.isSuccessful());
	}

	private void createAccountAndVerifyAndReload(UserAccount userAccount, BigDecimal balance) throws UsernameAlreadyExistsException, UserAccountNotFoundException, BitcoinException, InvalidUsernameException, InvalidEmailException, EmailAlreadyExistsException {
		assertTrue(userAccountService.createAccount(userAccount));
		userAccount = userAccountService.getByUsername(userAccount.getUsername());
		userAccount.setEmailVerified(true);
		userAccount.setBalance(balance);
		userAccountService.updateAccount(userAccount);
	}

	private HttpSession loginAndGetSession(String username, String plainTextPassword) throws Exception {
		HttpSession session = mockMvc.perform(post("/j_spring_security_check").secure(false).param("j_username", username).param("j_password", plainTextPassword))
				.andExpect(status().isOk())
				.andReturn()
				.getRequest()
				.getSession();

		return session;
	}

	@Test
	public void testResetPassword() throws Exception {
		createAccountAndVerifyAndReload(test30,BigDecimal.ONE);

		UserAccount fromDB = userAccountService.getByUsername(test30.getUsername());

		ObjectMapper mapper = new ObjectMapper();
		String emailAddress = fromDB.getEmail();
		String mappedString = mapper.writeValueAsString(emailAddress);

		MvcResult result = mockMvc.perform(post("/user/resetPasswordRequest").secure(false).contentType(MediaType.APPLICATION_JSON).content(mappedString))
				.andExpect(status().isOk())
				.andReturn();

		String resultAsString = result.getResponse().getContentAsString();
		CustomResponseObject transferObject = mapper.readValue(resultAsString, CustomResponseObject.class);		
		assertTrue(transferObject.isSuccessful());

		mappedString = mapper.writeValueAsString("wrong-email@noemail.com");
		result = mockMvc.perform(post("/user/resetPasswordRequest").secure(false).contentType(MediaType.APPLICATION_JSON).content(mappedString))
				.andExpect(status().isOk())
				.andReturn();
		resultAsString = result.getResponse().getContentAsString();
		transferObject = mapper.readValue(resultAsString, CustomResponseObject.class);		
		assertFalse(transferObject.isSuccessful());

		List<ResetPassword> list = userAccountService.getAllResetPassword();
		String token = null;
		for(int i=0; i<list.size();i++){
			if(list.get(i).getUserID() == fromDB.getId()){
				token = list.get(i).getToken();
			}
		}

		mockMvc.perform(get("/user/resetPassword/"+token).secure(false)).andExpect(status().isOk());

		mockMvc.perform(post("/user/resetPassword/setPassword").secure(false).param("token", token).param("pw1", "test").param("pw2", "test")).andExpect(status().isOk());

		mockMvc.perform(get("/user/resetPassword/" + token).secure(false))
				.andExpect(status().isOk())
				.andExpect(view().name("WrongToken"));

		HttpSession session = loginAndGetSession(test30.getUsername(), "test");

		MvcResult mvcResult3 = mockMvc.perform(get("/user/afterLogin").secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andExpect(content().contentType(TestUtil.APPLICATION_JSON_UTF8))
				.andReturn();

		String asString = mvcResult3.getResponse().getContentAsString();

		CustomResponseObject transferObject2 = mapper.readValue(asString, CustomResponseObject.class);

		assertEquals(true, transferObject2.isSuccessful());
		assertNotNull(transferObject2.getServerPublicKey());
		assertNotNull(transferObject2.getReadAccountTO());

		ReadAccountTransferObject readAccountTO = transferObject2.getReadAccountTO();

		assertEquals(test30.getUsername(), readAccountTO.getUserAccount().getUsername());
		assertEquals(test30.getEmail(), readAccountTO.getUserAccount().getEmail());

		logout(mvcResult3);
	}
	
	@Test
	public void testSaveUserPublicKey() throws Exception {
		String plainTextPassword = test31.getPassword();
		createAccountAndVerifyAndReload(test31, new BigDecimal(0.0));
		HttpSession session = loginAndGetSession(test31.getUsername(), plainTextPassword);
		
		KeyPair keyPair = KeyHandler.generateKeyPair();
		String encodedPublicKey = KeyHandler.encodePublicKey(keyPair.getPublic());
		CustomPublicKey upk = new CustomPublicKey((byte) 1, PKIAlgorithm.DEFAULT.getCode(), encodedPublicKey);
		
		ObjectMapper mapper = new ObjectMapper();
		String mappedString = mapper.writeValueAsString(upk);
		
		MvcResult result = mockMvc.perform(post("/user/savePublicKey").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(mappedString))
				.andExpect(status().isOk())
				.andReturn();

		String asString = result.getResponse().getContentAsString();

		CustomResponseObject transferObject = mapper.readValue(asString, CustomResponseObject.class);

		assertEquals(true, transferObject.isSuccessful());
		assertEquals(Type.SAVE_PUBLIC_KEY, transferObject.getType());
		assertEquals(1, Byte.parseByte(transferObject.getMessage()));
	}
	
	private static final BigDecimal TRANSACTION_AMOUNT = new BigDecimal(10.1).setScale(8, RoundingMode.HALF_UP);
	
	@Test
	public void testGetMainActivityRequests() throws Exception {
		assertTrue(userAccountService.createAccount(test32));
		test32 = userAccountService.getByUsername(test32.getUsername());
		test32.setEmailVerified(true);
		test32.setBalance(TRANSACTION_AMOUNT.multiply(new BigDecimal(10)));
		userAccountService.updateAccount(test32);
		
		String plainTextPw = test33.getPassword();
		assertTrue(userAccountService.createAccount(test33));
		test33 = userAccountService.getByUsername(test33.getUsername());
		test33.setEmailVerified(true);
		userAccountService.updateAccount(test33);
		
		KeyPair keyPairPayer = KeyHandler.generateKeyPair();
		byte keyNumberPayer = userAccountService.saveUserPublicKey(test32.getId(), PKIAlgorithm.DEFAULT, KeyHandler.encodePublicKey(keyPairPayer.getPublic()));
		
		ObjectMapper mapper = null;
		
		HttpSession session = loginAndGetSession(test32.getUsername(), plainTextPw);
		MvcResult mvcResult = null;
		for(int i = 0; i<5;i++){
			PaymentRequest paymentRequestPayer = new PaymentRequest(
					PKIAlgorithm.DEFAULT, 
					keyNumberPayer, 
					test32.getUsername(), 
					test33.getUsername(), 
					Currency.BTC, 
					Converter.getLongFromBigDecimal(TRANSACTION_AMOUNT),
					System.currentTimeMillis());
			paymentRequestPayer.sign(keyPairPayer.getPrivate());
			
			ServerPaymentRequest request = new ServerPaymentRequest(paymentRequestPayer);
			CreateTransactionTransferObject ctto = new CreateTransactionTransferObject(request);
			
			mapper = new ObjectMapper();
			String asString = mapper.writeValueAsString(ctto);
			
			mvcResult = mockMvc.perform(post("/transaction/create").secure(false).session((MockHttpSession) session).contentType(MediaType.APPLICATION_JSON).content(asString))
					.andExpect(status().isOk())
					.andReturn();
			
			CustomResponseObject cro = mapper.readValue(mvcResult.getResponse().getContentAsString(), CustomResponseObject.class);
			assertTrue(cro.isSuccessful());
			assertNotNull(cro.getServerPaymentResponse());
			ServerPaymentResponse response = DecoderFactory.decode(ServerPaymentResponse.class, cro.getServerPaymentResponse());
			assertNotNull(response);
			assertNotNull(response.getPaymentResponsePayer());
			assertEquals(ServerResponseStatus.SUCCESS, response.getPaymentResponsePayer().getStatus());
		}
		
		mvcResult = mockMvc.perform(get("/user/mainActivityRequests")
				.secure(false).session((MockHttpSession) session))
				.andExpect(status().isOk())
				.andReturn();
		
		CustomResponseObject cro2 = mapper.readValue(mvcResult.getResponse().getContentAsString(), CustomResponseObject.class);
		assertTrue(cro2.isSuccessful());
		BigDecimal exchangeRate = new BigDecimal(cro2.getMessage());
		assertTrue(exchangeRate.compareTo(BigDecimal.ZERO) >= 0);
		
		assertNotNull(cro2.getBalance());
		
		GetHistoryTransferObject ghto = cro2.getGetHistoryTO();
		assertNotNull(ghto);
		assertEquals(5, ghto.getTransactionHistory().size());
		assertEquals(0, ghto.getPayInTransactionHistory().size());
		assertEquals(0, ghto.getPayOutTransactionHistory().size());
		
		logout(mvcResult);
		
		mvcResult = mockMvc.perform(get("/user/mainActivityRequests").secure(false).session((MockHttpSession) session))
				.andExpect(status().isUnauthorized())
				.andReturn();
	}

	private void logout(MvcResult result) {
		result.getRequest().getSession().invalidate();
	}

	

}
