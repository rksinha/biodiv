package speciespage

import grails.plugins.springsecurity.ui.SpringSecurityUiService;

import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils;
import org.codehaus.groovy.grails.plugins.springsecurity.ui.RegistrationCode;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import species.auth.SUser;

class SUserService extends SpringSecurityUiService {

	static transactional = false

	def grailsApplication

	/**
	 * 
	 */
	SUser create(Map propsMap) {
		propsMap = propsMap ?: [:];
		
		propsMap.remove('metaClass')
		propsMap.remove('class')

		String userDomainClassName = SpringSecurityUtils.securityConfig.userLookup.userDomainClassName

		Class<?> UserDomainClass = grailsApplication.getDomainClass(userDomainClassName).clazz
		if (!UserDomainClass) {
			log.error("Can't find user domain: $userDomainClassName")
			return null
		}

		def user = UserDomainClass.newInstance(propsMap);
		user.enabled = true;
		return user;
	}

	/**
	 * 
	 */
	void mergeUserDetails(details, user) {
		
	}

	/**
	 * 
	 * @param user
	 * @param cleartextPassword
	 * @param salt
	 * @return
	 */
	SUser save(user) {
		if (!user.save()) {
			warnErrors user, messageSource
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
			return null
		}
		return user
	}

	/**
	 * 
	 * @param user
	 * @return
	 */
	RegistrationCode register(String username) {
		if(!username) return null;

		def registrationCode = new RegistrationCode(username: username)
		if (!registrationCode.save()) {
			warnErrors registrationCode, messageSource
			TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
		}

		registrationCode
	}

	/**
	 * 
	 * @param user
	 */
	void assignRoles(SUser user) {
		def securityConf = SpringSecurityUtils.securityConfig

		def defaultRoleNames = securityConf.ui.register.defaultRoleNames;

		Class<?> PersonRole = grailsApplication.getDomainClass(securityConf.userLookup.authorityJoinClassName).clazz
		Class<?> Authority = grailsApplication.getDomainClass(securityConf.authority.className).clazz
		PersonRole.withTransaction { status ->
			defaultRoleNames.each { String roleName ->
				String findByField = securityConf.authority.nameField[0].toUpperCase() + securityConf.authority.nameField.substring(1)
				def auth = Authority."findBy${findByField}"(roleName)
				if (auth) {
					PersonRole.create(user, auth)
				} else {
					log.error("Can't find authority for name '$roleName'")
				}
			}
		}
	}
}