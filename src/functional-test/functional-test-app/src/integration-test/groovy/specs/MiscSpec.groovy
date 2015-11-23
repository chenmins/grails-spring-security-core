package specs

import pages.IndexPage

class MiscSpec extends AbstractHyphenatedSecuritySpec {

	void 'salted password'() {

		given:
		String username = 'testuser_books_and_movies'
		def passwordEncoder = createSha256Encoder()

		when:
		String hashedPassword = getUserProperty(username, 'password')
		String notSalted = passwordEncoder.encodePassword('password', null)
		String salted = passwordEncoder.encodePassword('password', username)

		then:
		salted == hashedPassword
		notSalted != hashedPassword
	}

	void 'switch user'() {

		when:
		login 'admin'

		then:
		at IndexPage

		// verify logged in
		when:
		go 'secure-annotated'

		then:
		assertContentContains 'you have ROLE_ADMIN'

		when:
		String auth = getSessionValue('SPRING_SECURITY_CONTEXT')

		then:
		auth.contains 'Username: admin'
		auth.contains 'Authenticated: true'
		auth.contains 'ROLE_ADMIN'
		auth.contains 'ROLE_USER' // new, added since inferred from role hierarchy
		!auth.contains('ROLE_PREVIOUS_ADMINISTRATOR')

		// switch
		when:
		go 'j_spring_security_switch_user?j_username=testuser'

		then:
		assertContentContains 'Available Controllers:'

		// verify logged in as testuser

		when:
		auth = getSessionValue('SPRING_SECURITY_CONTEXT')

		then:
		auth.contains 'Username: testuser'
		auth.contains 'Authenticated: true'
		auth.contains 'ROLE_USER'
		auth.contains 'ROLE_PREVIOUS_ADMINISTRATOR'

		when:
		go 'secure-annotated/user-action'

		then:
		assertContentContains 'you have ROLE_USER'

		// verify not logged in as admin
		when:
		go 'secure-annotated/admin-either'

		then:
		assertContentContains "Sorry, you're not authorized to view this page."

		// switch back
		when:
		go 'j_spring_security_exit_user'

		then:
		assertContentContains 'Available Controllers:'

		// verify logged in as admin
		when:
		go 'secure-annotated/admin-either'

		then:
		assertContentContains 'you have ROLE_ADMIN'

		when:
		auth = getSessionValue('SPRING_SECURITY_CONTEXT')

		then:
		auth.contains 'Username: admin'
		auth.contains 'Authenticated: true'
		auth.contains 'ROLE_ADMIN'
		auth.contains 'ROLE_USER'
		!auth.contains('ROLE_PREVIOUS_ADMINISTRATOR')
	}

	void 'hierarchical roles'() {

		when:
		login 'admin'

		then:
		at IndexPage

		// verify logged in
		when:
		go 'secure-annotated'

		then:
		assertContentContains 'you have ROLE_ADMIN'

		when:
		String auth = getSessionValue('SPRING_SECURITY_CONTEXT')

		then:
		auth.contains 'Authenticated: true'
		auth.contains 'ROLE_USER'

		// now get an action that's ROLE_USER only
		when:
		go 'secure-annotated/user-action'

		then:
		assertContentContains 'you have ROLE_USER'
	}

	void 'taglibs unauthenticated'() {

		when:
		go 'tag-lib-test/test'

		then:
		assertContentDoesNotContain 'user and admin'
		assertContentDoesNotContain 'user and admin and foo'
		assertContentContains 'not user and not admin'
		assertContentDoesNotContain 'user or admin'
		assertContentContains 'accountNonExpired: "not logged in"'
		assertContentContains 'id: "not logged in"'
		assertContentContains 'Username is ""'
		assertContentDoesNotContain 'logged in true'
		assertContentContains 'logged in false'
		assertContentDoesNotContain 'switched true'
		assertContentContains 'switched false'
		assertContentContains 'switched original username ""'

		assertContentDoesNotContain 'access with role user: true'
		assertContentDoesNotContain 'access with role admin: true'
		assertContentContains 'access with role user: false'
		assertContentContains 'access with role admin: false'

		assertContentContains 'Can access /login/auth'
		assertContentDoesNotContain 'Can access /secure-annotated'
		assertContentDoesNotContain 'Cannot access /login/auth'
		assertContentContains 'Cannot access /secure-annotated'

		assertContentContains 'anonymous access: true'
		assertContentContains 'Can access /tag-lib-test/test'
		assertContentDoesNotContain 'anonymous access: false'
		assertContentDoesNotContain 'Cannot access /tag-lib-test/test'
	}

	void 'taglibs user'() {

		when:
		login 'testuser'

		then:
		at IndexPage

		when:
		go 'tag-lib-test/test'

		then:
		assertContentDoesNotContain 'user and admin'
		assertContentDoesNotContain 'user and admin and foo'
		assertContentDoesNotContain 'not user and not admin'
		assertContentContains 'user or admin'
		assertContentContains 'accountNonExpired: "true"'
		assertContentDoesNotContain 'id: "not logged in"' // can't test on exact id, don't know what it is
		assertContentContains 'Username is "testuser"'
		assertContentContains 'logged in true'
		assertContentDoesNotContain 'logged in false'
		assertContentDoesNotContain 'switched true'
		assertContentContains 'switched false'
		assertContentContains 'switched original username ""'

		assertContentContains 'access with role user: true'
		assertContentDoesNotContain 'access with role admin: true'
		assertContentDoesNotContain 'access with role user: false'
		assertContentContains 'access with role admin: false'

		assertContentContains 'Can access /login/auth'
		assertContentDoesNotContain 'Can access /secure-annotated'
		assertContentDoesNotContain 'Cannot access /login/auth'
		assertContentContains 'Cannot access /secure-annotated'

		assertContentContains 'anonymous access: false'
		assertContentContains 'Can access /tag-lib-test/test'
		assertContentDoesNotContain 'anonymous access: true'
	}

	void 'taglibs admin'() {

		when:
		login 'admin'

		then:
		at IndexPage

		when:
		go 'tag-lib-test/test'

		then:
		assertContentContains 'user and admin'
		assertContentDoesNotContain 'user and admin and foo'
		assertContentDoesNotContain 'not user and not admin'
		assertContentContains 'user or admin'
		assertContentContains 'accountNonExpired: "true"'
		assertContentDoesNotContain 'id: "not logged in"' // can't test on exact id, don't know what it is
		assertContentContains 'Username is "admin"'

		assertContentContains 'logged in true'
		assertContentDoesNotContain 'logged in false'
		assertContentDoesNotContain 'switched true'
		assertContentContains 'switched false'
		assertContentContains 'switched original username ""'

		assertContentContains 'access with role user: true'
		assertContentContains 'access with role admin: true'
		assertContentDoesNotContain 'access with role user: false'
		assertContentDoesNotContain 'access with role admin: false'

		assertContentContains 'Can access /login/auth'
		assertContentContains 'Can access /secure-annotated'
		assertContentDoesNotContain 'Cannot access /login/auth'
		assertContentDoesNotContain 'Cannot access /secure-annotated'

		assertContentContains 'anonymous access: false'
		assertContentContains 'Can access /tag-lib-test/test'
		assertContentDoesNotContain 'anonymous access: true'
		assertContentDoesNotContain 'Cannot access /tag-lib-test/test'
	}

	void 'metaclass methods unauthenticated'() {

		when:
		go 'tag-lib-test/testMetaclassMethods'

		then:
		assertContentContains 'getPrincipal: org.springframework.security.core.userdetails.User'
		assertContentContains 'Username: __grails.anonymous.user__'
		assertContentContains 'Granted Authorities: ROLE_ANONYMOUS'
		assertContentContains 'isLoggedIn: false'
		assertContentContains 'loggedIn: false'
		assertContentContains 'getAuthenticatedUser: null'
		assertContentContains 'authenticatedUser: null'
	}

	void 'metaclass methods authenticated'() {

		when:
		login 'admin'

		then:
		at IndexPage

		when:
		go 'tag-lib-test/test-metaclass-methods'

		then:
		assertContentContains 'getPrincipal: grails.plugin.springsecurity.userdetails.GrailsUser'
		assertContentContains 'principal: grails.plugin.springsecurity.userdetails.GrailsUser'
		assertContentContains 'Username: admin'
		assertContentContains 'isLoggedIn: true'
		assertContentContains 'loggedIn: true'
		assertContentContains 'getAuthenticatedUser: admin'
		assertContentContains 'authenticatedUser: admin'
	}

	void 'test hyphenated'() {

		when:
		go 'foo-bar'

		then:
		assertContentContains 'Please Login'

		when:
		go 'foo-bar/index'

		then:
		assertContentContains 'Please Login'

		when:
		go 'foo-bar/bar-foo'

		then:
		assertContentContains 'Please Login'

		when:
		logout()
		login 'admin'

		then:
		at IndexPage

		when:
		go 'foo-bar'

		then:
		assertContentContains 'INDEX'

		when:
		go 'foo-bar/index'

		then:
		assertContentContains 'INDEX'

		when:
		go 'foo-bar/bar-foo'

		then:
		assertContentContains 'barFoo'
	}
}