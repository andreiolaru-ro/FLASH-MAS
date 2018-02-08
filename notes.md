




CLI
===

all name:val pairs belong in an element

all elements belong in a category; all categories belong in elements or at the root level

CLI arguments			 ::= scenario-file? category-description*
category-description	 ::= -category element element_description
element_description		 ::= (par:val)* category-description*
element					 ::= 		// basically anything, but interpreted as:
							type:name			[depending on category, can mean loader:id or type:id]
							type:				[an unnamed element with the specified type/loader]
							name				[depending on category, a named element of the default type or an unnamed element with this type]
													[the exact variant is decided in Boot, not in the CLI parser]