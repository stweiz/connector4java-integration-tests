/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.osiam.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.osiam.client.exception.ConnectionInitializationException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.oauth.Scope;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

import com.github.springtestdbunit.DbUnitTestExecutionListener;
import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseTearDown;

/**
 * Test for resource owner password credentials grant.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/context.xml")
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class,
        DbUnitTestExecutionListener.class })
@DatabaseSetup("/database_seed_login_password_credentials.xml")
@DatabaseTearDown(value = "/database_tear_down.xml", type = DatabaseOperation.DELETE_ALL)
public class LoginResourceOwnerPasswordCredentialsIT {

    protected static final String AUTH_ENDPOINT_ADDRESS = "http://localhost:8180/osiam-auth-server";
    protected static final String RESOURCE_ENDPOINT_ADDRESS = "http://localhost:8180/osiam-resource-server";
    protected String clientId = "example-client";
    protected String clientSecret = "secret";

    @Test
    public void login_with_resource_owner_password_credentials_grant_should_provide_an_refresh_token() {
        OsiamConnector osiamConnector = createOsiamConnector();

        AccessToken at = osiamConnector.retrieveAccessToken("marissa", "koala", Scope.ALL);

        assertNotNull("The hole access token object was null.", at);
        assertNotNull("The refresh token was null.", at.getRefreshToken());
    }

    @Test
    public void login_with_two_users_works() {
        OsiamConnector osiamConnector = createOsiamConnector();
        AccessToken at = osiamConnector.retrieveAccessToken("marissa", "koala", Scope.ALL);

        OsiamConnector osiamConnector2 = createOsiamConnector();
        AccessToken at2 = osiamConnector2.retrieveAccessToken("marissa", "koala", Scope.ALL);

        assertNotNull(at);
        assertNotNull(at2);
    }

    @Test
    public void multiple_failed_logins(){
        OsiamConnector osiamConnector = createOsiamConnector();

        for(int i=0; i < 3; i++){
            try{
                osiamConnector.retrieveAccessToken("marissa03", "wrongPassword", Scope.ALL);
            }catch(ConnectionInitializationException e){
                assertTrue(e.getMessage().contains("Bad credentials"));
            }
        }

        try{
            osiamConnector.retrieveAccessToken("marissa03", "koala", Scope.ALL);
        }catch(ConnectionInitializationException e){
            assertTrue(e.getMessage().contains("temporary locked"));
        }
    }

    @Test
    public void multiple_failed_logins_reset(){
        OsiamConnector osiamConnector = createOsiamConnector();

        for(int i=0; i < 2; i++){
            try{
                osiamConnector.retrieveAccessToken("marissa04", "wrongPassword", Scope.ALL);
            }catch(ConnectionInitializationException e){
                assertTrue(e.getMessage().contains("Bad credentials"));
            }
        }

        osiamConnector.retrieveAccessToken("marissa04", "koala", Scope.ALL);

        for(int i=0; i < 2; i++){
            try{
                osiamConnector.retrieveAccessToken("marissa04", "wrongPassword", Scope.ALL);
            }catch(ConnectionInitializationException e){
                assertTrue(e.getMessage().contains("Bad credentials"));
            }
        }
        osiamConnector.retrieveAccessToken("marissa04", "koala", Scope.ALL);
    }

    @Test
    public void multiple_failed_logins_wait(){
        OsiamConnector osiamConnector = createOsiamConnector();

        for(int i=0; i < 3; i++){
            try{
                osiamConnector.retrieveAccessToken("marissa05", "wrongPassword", Scope.ALL);
            }catch(ConnectionInitializationException e){
                assertTrue(e.getMessage().contains("Bad credentials"));
            }
        }

        try {
            Thread.sleep(3000);
            osiamConnector.retrieveAccessToken("marissa05", "koala", Scope.ALL);
        } catch (InterruptedException e) {}
    }

    private OsiamConnector createOsiamConnector() {
        return new OsiamConnector.Builder()
                .setAuthServerEndpoint(AUTH_ENDPOINT_ADDRESS)
                .setResourceServerEndpoint(RESOURCE_ENDPOINT_ADDRESS)
                .setClientId(clientId).setClientSecret(clientSecret)
                .build();
    }
}