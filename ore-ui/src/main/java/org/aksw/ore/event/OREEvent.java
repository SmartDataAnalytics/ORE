package org.aksw.ore.event;

import org.aksw.ore.model.Knowledgebase;
import org.aksw.ore.view.OREViewType;

/*
 * Event bus events used in ORE are listed here as inner classes.
 */
public abstract class OREEvent {

    public static final class UserLoginRequestedEvent {
        private final String userName, password;

        public UserLoginRequestedEvent(final String userName,
                final String password) {
            this.userName = userName;
            this.password = password;
        }

        public String getUserName() {
            return userName;
        }

        public String getPassword() {
            return password;
        }
    }
    
    public static final class KnowledgebaseChangedEvent {
    	private final Knowledgebase kb;
    	
		public KnowledgebaseChangedEvent(Knowledgebase kb) {
			this.kb = kb;
		}
		public Knowledgebase getKb() {
			return kb;
		}
    }

    public static class BrowserResizeEvent {

    }

    public static class UserLoggedOutEvent {

    }

    public static class NotificationsCountUpdatedEvent {
    }


    public static final class PostViewChangeEvent {
        private final OREViewType view;

        public PostViewChangeEvent(final OREViewType view) {
            this.view = view;
        }

        public OREViewType getView() {
            return view;
        }
    }

    public static class CloseOpenWindowsEvent {
    }
}