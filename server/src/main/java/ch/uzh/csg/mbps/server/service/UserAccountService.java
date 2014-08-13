package ch.uzh.csg.mbps.server.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.uzh.csg.mbps.customserialization.PKIAlgorithm;

import org.hibernate.HibernateException;

import ch.uzh.csg.mbps.server.clientinterface.IUserAccount;
import ch.uzh.csg.mbps.server.dao.UserAccountDAO;
import ch.uzh.csg.mbps.server.dao.UserPublicKeyDAO;
import ch.uzh.csg.mbps.server.domain.AdminRole;
import ch.uzh.csg.mbps.server.domain.ResetPassword;
import ch.uzh.csg.mbps.server.domain.UserAccount;
import ch.uzh.csg.mbps.server.util.BitcoindController;
import ch.uzh.csg.mbps.server.util.Config;
import ch.uzh.csg.mbps.server.util.CustomPasswordEncoder;
import ch.uzh.csg.mbps.server.util.Emailer;
import ch.uzh.csg.mbps.server.util.PasswordMatcher;
import ch.uzh.csg.mbps.server.util.UserModel;
import ch.uzh.csg.mbps.server.util.UserRoles;
import ch.uzh.csg.mbps.server.util.UserRoles.Role;
import ch.uzh.csg.mbps.server.util.exceptions.BalanceNotZeroException;
import ch.uzh.csg.mbps.server.util.exceptions.EmailAlreadyExistsException;
import ch.uzh.csg.mbps.server.util.exceptions.InvalidEmailException;
import ch.uzh.csg.mbps.server.util.exceptions.InvalidUsernameException;
import ch.uzh.csg.mbps.server.util.exceptions.UserAccountNotFoundException;
import ch.uzh.csg.mbps.server.util.exceptions.UsernameAlreadyExistsException;
import ch.uzh.csg.mbps.server.util.exceptions.VerificationTokenNotFoundException;

import com.azazar.bitcoin.jsonrpcclient.BitcoinException;

/**
 * Service class for {@link UserAccount}.
 */
public class UserAccountService implements IUserAccount {
	private static UserAccountService userAccountService;
	private static boolean TESTING_MODE = false;

	private UserAccountService() {
	}

	/**
	 * Returns new or existing instance of {@link UserAccountService}.
	 * 
	 * @return instance of UserAccountService
	 */
	public static UserAccountService getInstance() {
		if (userAccountService == null) {
			userAccountService = new UserAccountService();
		}
			
		return userAccountService;
	}
	
	/**
	 * Enables testing mode for JUnit Tests.
	 */
	public static void enableTestingMode() {
		TESTING_MODE = true;
	}
	
	public static boolean isTestingMode(){
		return TESTING_MODE;
	}

	/**
	 * Disables testing mode for JUnit Tests.
	 */
	public static void disableTestingMode() {
		TESTING_MODE = false;
	}

	@Override
	public boolean createAccount(UserAccount userAccount) throws UsernameAlreadyExistsException, BitcoinException, InvalidUsernameException, InvalidEmailException, EmailAlreadyExistsException {
		if (TESTING_MODE)
			return createAccount(userAccount, "fake-address");
		else
			return createAccount(userAccount, getNewPaymentAddress());
	}
	
	private boolean createAccount(UserAccount userAccount, String paymentAddress) throws UsernameAlreadyExistsException, BitcoinException, InvalidUsernameException, InvalidEmailException, EmailAlreadyExistsException {
		UserAccount fromDB = null;
		
		userAccount.setUsername(userAccount.getUsername().trim());
		String username = userAccount.getUsername();
		String email = userAccount.getEmail();
		byte roles = userAccount.getRoles();
		
		if (username == null)
			throw new InvalidUsernameException();

		if (email == null)
			throw new InvalidEmailException();

		if (!username.matches(Config.USERNAME_REGEX))
			throw new InvalidUsernameException();
		
		if (!email.matches(Config.EMAIL_REGEX))
			throw new InvalidEmailException();
		
		try {
			//see for matches in db ignoring cases and deletion status
			fromDB = getByUsernameIgnoreCaseAndDeletedFlag(userAccount.getUsername());
			if (!fromDB.isEmailVerified()) {
				resendVerificationEmail(fromDB);
			}
			throw new UsernameAlreadyExistsException(userAccount.getUsername());
		} catch (UserAccountNotFoundException e) {
			//do nothing, since this happens when a new account is created with a unique username
		}
		
		try {
			//see for emailaddress matches in db ignoring cases and deletion status
			getByEmailIgnoreCaseAndDeletedFlag(email);
			throw new EmailAlreadyExistsException(email);
		} catch (UserAccountNotFoundException e) {
			//do nothing, since this happens when a new account is created with a unique email address
		}
		
		userAccount = new UserAccount(userAccount.getUsername(), userAccount.getEmail(), userAccount.getPassword());
		
		String passwordHash = CustomPasswordEncoder.getEncodedPassword(userAccount.getPassword());
		userAccount.setPassword(passwordHash);
		userAccount.setPaymentAddress(paymentAddress);
		
		if (roles < 1 || roles > 3)
			roles = Role.USER.getCode();
		
		userAccount.setRoles(roles);
		
		String token = java.util.UUID.randomUUID().toString();
		try {
			UserAccountDAO.createAccount(userAccount, token);
			sendEmailVerificationLink(token, userAccount.getEmail());
		} catch (HibernateException e) {
			return false;
		}
		return true;
	}
	
	private String getNewPaymentAddress() throws BitcoinException {
		return BitcoindController.getNewAddress();
	}
	
	private void sendEmailVerificationLink(String token, String email){
		Emailer.sendEmailConfirmationLink(token, email);
	}
	
	public void resendVerificationEmail(UserAccount userAccount) {
		String token;
		try {
			token = UserAccountDAO.getVerificationTokenByUserId(userAccount.getId());
			Emailer.sendEmailConfirmationLink(token, userAccount.getEmail());
		} catch (VerificationTokenNotFoundException e) {
			token = java.util.UUID.randomUUID().toString();
			try {
				UserAccountDAO.createEmailVerificationToken(userAccount.getId(), token);
				Emailer.sendEmailConfirmationLink(token, userAccount.getEmail());
			} catch (HibernateException e1) { 
			}
		}
	}

	@Override
	public UserAccount getByUsername(String username) throws UserAccountNotFoundException {
		return UserAccountDAO.getByUsername(username);
	}
	
	private UserAccount getByUsernameIgnoreCaseAndDeletedFlag(String username) throws UserAccountNotFoundException {
		return UserAccountDAO.getByUsernameIgnoreCaseAndDeletedFlag(username);
	}
	
	private UserAccount getByEmailIgnoreCaseAndDeletedFlag(String email) throws UserAccountNotFoundException{
		return UserAccountDAO.getByEmailIgnoreCaseAndDeletedFlag(email);
	}

	@Override
	public UserAccount getById(long id) throws UserAccountNotFoundException {
		return UserAccountDAO.getById(id);
	}

	@Override
	public boolean updateAccount(String username, UserAccount updatedAccount) throws UserAccountNotFoundException{
		UserAccount userAccount = getByUsername(username);

		if (updatedAccount.getEmail() != null && !updatedAccount.getEmail().isEmpty())
			userAccount.setEmail(updatedAccount.getEmail());

		if (updatedAccount.getPassword() != null && !updatedAccount.getPassword().isEmpty())
			userAccount.setPassword(CustomPasswordEncoder.getEncodedPassword(updatedAccount.getPassword()));
		
		//TODO: no logic behind....changes the role always to user 
//		if (UserRoles.isValidRole(updatedAccount.getRoles()))
//			userAccount.setRoles(updatedAccount.getRoles());

		try {
			UserAccountDAO.updateAccount(userAccount);
			return true;
		} catch (HibernateException e) {
			return false;
		}
	}

	@Override
	public boolean delete(String username) throws UserAccountNotFoundException, BalanceNotZeroException {
		try {
			UserAccountDAO.delete(username);
			return true;
		} catch (HibernateException e) {
			return false;
		}
	}

	@Override
	public boolean verifyEmailAddress(String verificationToken) {
		try {
			UserAccountDAO.verifyEmail(verificationToken);
			return true;
		} catch (UserAccountNotFoundException | HibernateException | VerificationTokenNotFoundException e) {
			return false;
		} 
	}

	/**
	 * Generates new {@link ResetPassword} Entry and sends Email to user with token
	 * @param emailAddress
	 * @throws UserAccountNotFoundException
	 */
	public void resetPasswordRequest(String emailAddress) throws UserAccountNotFoundException {
		UserAccount user = UserAccountDAO.getByEmail(emailAddress);
		String token = java.util.UUID.randomUUID().toString();
		UserAccountDAO.createPasswordResetToken(user, token);
		
		Emailer.sendResetPasswordLink(user, token);
	}
	
	/**
	 * Checks if token is saved in table and still valid (younger than 1h)
	 * @param resetPasswordToken
	 * @return
	 */
	public boolean isValidResetPasswordLink(String resetPasswordToken) {
		try {
			ResetPassword resetPassword = UserAccountDAO.getResetPassword(resetPasswordToken);
			if (resetPassword == null) {
				return false;
			} else {
				// checks if token has been created during the last 1h
				if (resetPassword.getCreationDate().getTime() >= (new Date().getTime() - Config.VALID_TOKEN_LIMIT)) {
					return true;
				} else {
					return false;
				}
			}
		} catch (VerificationTokenNotFoundException e) {
			return false;
		} 
	}	
	
	/**
	 * Deletes every time when a password is reseted the old entries ({@link ResetPassword}s) (older than 24h) from the table.
	 */
	public void deleteOldResetPasswords(){
		Date currentDate = new Date();
		List<ResetPassword> list = UserAccountDAO.getAllResetPassword();
		for(int i=0; i< list.size();i++){
			if(list.get(i).getCreationDate().getTime() < (currentDate.getTime() - Config.DELETE_TOKEN_LIMIT)){
				try {
					UserAccountDAO.deleteResetPassword(list.get(i).getToken());
				} catch (VerificationTokenNotFoundException e) {
				}
			}
		}
	}
	
	/**
	 * Returns {@link UserAccount} which belongs to ResetPassword-Token.
	 * @param token
	 * @return UserAccount
	 * @throws VerificationTokenNotFoundException
	 * @throws UserAccountNotFoundException
	 */
	public UserAccount getByResetPasswordToken(String token) throws VerificationTokenNotFoundException, UserAccountNotFoundException{
		return UserAccountDAO.getByResetPasswordToken(token);
	}
	
	/**
	 * Deletes all old tokens first, afterwards resets the userpassword and deletes the used token.
	 * @param matcher
	 * @return true if password has successfully been reseted
	 */
	public boolean resetPassword(PasswordMatcher matcher){
		if (!UserAccountService.getInstance().isValidResetPasswordLink(matcher.getToken())) {
			return false;
		}
		
		//clean table and delete all old tokens
		deleteOldResetPasswords();
		
		UserAccount user;
		if (matcher.compare()){
			try {
				user = UserAccountService.getInstance().getByResetPasswordToken(matcher.getToken());
				user.setPassword(matcher.getPw1());
				UserAccountService.getInstance().updateAccount(user.getUsername(), user);
				UserAccountDAO.deleteResetPassword(matcher.getToken());		
				return true;
			} catch (VerificationTokenNotFoundException | UserAccountNotFoundException e) {
				return false;
			}
		} else {
			return false;
		}
	}
	
	/**
	 * Stores a public key on the database and maps this public key to a user
	 * account.
	 * 
	 * @param userId
	 *            the id of the user account
	 * @param algorithm
	 *            the {@link PKIAlgorithm} used to generate the key
	 * @param publicKey
	 *            the base64 encoded public key
	 * @return the key number, indicating the (incremented) position this public
	 *         key has in a list of public keys mapped to this user account
	 * @throws UserAccountNotFoundException
	 */
	public byte saveUserPublicKey(long userId, PKIAlgorithm algorithm, String publicKey) throws UserAccountNotFoundException {
		return UserPublicKeyDAO.saveUserPublicKey(userId, algorithm, publicKey);
	}
	
	/**
	 * Returns sum of Balance of each user account in the system.
	 * @return Sum of each Balance
	 */
	public BigDecimal getSumOfAllAccounts(){
		List<UserAccount> users = UserAccountDAO.getAllUserAccounts();
		
		BigDecimal sum = BigDecimal.ZERO;
		for(UserAccount user: users){
			sum = sum.add(user.getBalance());
		}
		return sum;
	}
	
	//TODO: mehmet
	/**
	 * Gets all users which have the role as admin
	 * @return
	 */
	public List<UserAccount> getAdmins(){
		List<UserAccount> users = new ArrayList<UserAccount>(); 
		users = UserAccountDAO.getAllUsersByRoles(Role.ADMIN);
		users.addAll(UserAccountDAO.getAllUsersByRoles(Role.BOTH));
		return users;
	}
	
	//TODO: mehmet
	/**
	 * Gets all users which have the role as user
	 * @return
	 */
	public List<UserAccount> getUsers(){
		List<UserAccount> users = new ArrayList<UserAccount>(); 
		users = UserAccountDAO.getAllUsersByRoles(Role.USER);
		users.addAll(UserAccountDAO.getAllUsersByRoles(Role.BOTH));
		return users;
	}
	
	//TODO: mehmet
	/**
	 * 
	 * @param username
	 * @return
	 */
	public UserModel getLoggedAdmin(String username) {
		UserAccount account = null;
		try {
			account = UserAccountDAO.getByUsername(username);
		} catch (UserAccountNotFoundException e) {
			return null;
		}
		return new UserModel(account.getId(), account.getUsername(), account.getCreationDate(), 
				account.getEmail(), account.getPassword(), account.getPaymentAddress(), account.getRoles());
	}

	//TODO: mehmet test & javadoc
	/**
	 * 
	 * @param email
	 * @return
	 * @throws UserAccountNotFoundException 
	 */
	public UserAccount getByEmail(String email) throws UserAccountNotFoundException {
		return UserAccountDAO.getByEmail(email);
	}

	//TODO: mehmet test & javadoc
	/**
	 * 
	 * @param email
	 * @throws UserAccountNotFoundException 
	 */
	public static void changeRoleBoth(String emailAddress) throws UserAccountNotFoundException {
		UserAccount user = UserAccountDAO.getByEmail(emailAddress);
		Emailer.sendUpdateRoleBothLink(user);
	}

	//TODO: mehmet Test & javadoc
	/**
	 * 
	 * @param email
	 * @throws UserAccountNotFoundException 
	 */
	public static void changeRoleAdmin(String emailAddress) throws UserAccountNotFoundException {
		UserAccount user = UserAccountDAO.getByEmail(emailAddress);
		String token = java.util.UUID.randomUUID().toString();
		UserAccountDAO.createAdminToken(user, token);
		
		Emailer.sendCreateRoleAdminLink(user, token);
	}
	
	/**
	 * Checks if token is saved in table and still valid (younger than 1h)
	 * @param adminToken
	 * @return
	 */
	public boolean isValidAdminLink(String adminToken) {
		try {
			AdminRole adminRole = UserAccountDAO.getCreateAdmin(adminToken);
			if (adminRole == null) {
				return false;
			} else {
				// checks if token has been created during the last 1h
				if (adminRole.getCreationDate().getTime() >= (new Date().getTime() - Config.VALID_TOKEN_LIMIT)) {
					return true;
				} else {
					return false;
				}
			}
		} catch (VerificationTokenNotFoundException e) {
			return false;
		} 
	}
}
