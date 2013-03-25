package org.aksw.mole.ore.widget;

import org.vaadin.appfoundation.authentication.exceptions.AccountLockedException;
import org.vaadin.appfoundation.authentication.exceptions.InvalidCredentialsException;
import org.vaadin.appfoundation.authentication.util.AuthenticationUtil;

import com.vaadin.ui.LoginForm;
import com.vaadin.ui.LoginForm.LoginEvent;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

public class LoginDialog extends Window {
	
	private static final String DIALOG_TITLE = "Login";
	
	public LoginDialog() {
		super(DIALOG_TITLE);
		setModal(true);
		
		initUI();
	}
	
	private void initUI(){
		VerticalLayout l = new VerticalLayout();
		LoginForm login = new LoginForm();
        login.setWidth("100%");
        login.setHeight("300px");
        login.addListener(new LoginForm.LoginListener() {
            public void onLogin(LoginEvent event) {
            	String username = event.getLoginParameter("username");
                String password = event.getLoginParameter("password");
            	
                try {
                    AuthenticationUtil.authenticate(username, password);
                    getWindow().showNotification("Successfully logged in.");
                } catch (InvalidCredentialsException e) {
                	getWindow().showNotification("Error: Incorrect username or password", Notification.TYPE_ERROR_MESSAGE);
                } catch (AccountLockedException e) {
					e.printStackTrace();
				}
            }
        });
        l.addComponent(login);
        
        addComponent(l);
	}

}
