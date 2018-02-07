




CLI
===

all name:val pairs belong in an element

all elements belong in a category

CLI arguments			 ::= scenario-file? category-description*
category-description	 ::= -category element element_description
element_description		 ::= (par:val)* category-description*
element					 ::= 		// basically anything, but interpreted as:
							type:name			[depending on category, can mean loader:id or type:id]
							type:				[an unnamed element with the specified type/loader]
							name				[depending on the platform, a named element of the default type or an unnamed element with this type]