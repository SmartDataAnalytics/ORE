package org.aksw.ore.rendering;
public enum Syntax{
		MANCHESTER("Manchester Syntax"), DL("Description Logic Syntax");
		
		private String label;
		private String description;
		
		private Syntax(String label) {
			this(label, label);
		}

		private Syntax(String label, String description) {
			this.label = label;
			this.description = description;
		}
		
		/**
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}
		
		/**
		 * @return the description
		 */
		public String getDescription() {
			return description;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return label;
		}
		
	}