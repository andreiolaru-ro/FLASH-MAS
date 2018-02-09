


XML
===

the default loader / platform / etc is always the first




CLI
===

all name:val pairs belong in an element

all elements belong in a category; all categories belong in elements or at the root level

	CLI arguments			 ::= scenario-file? category-description*
	category-description	 ::= -category element element_description
	element_description		 ::= (par:val)* category-description*
	element					 ::= 		// basically anything, but interpreted as:
								type:name			[depending on category, can mean loader:id or type:id category:type]
								type:				[an unnamed element with the specified type/loader]
								name				[depending on category, a named element of the default type or an unnamed element with this type]
														[the exact variant is decided in Boot, not in the CLI parser]
														
**TODO:**
 * fuse trees from XML and CLI in the case of elements with optional parents
 * implement q attribute in XML that is parsed by the CLI parser
 * implement special categories such as
  * -root (resets context stack)
  * -all-of:always category par:val* (copy/fuse the subordinate tree to all children of category)
  * -all-of:missing category par:val* (insert the values in the subordinate tree in all children of category, but only if no such parameter is already defined).