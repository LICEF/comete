Based on content negotiation mechanism, Pubby serves different formats depending on clients requesting URIs on it :
•RDF format for standalone applications
•HTML description page for browsers

The case of Internet Explorer
-----------------------------

However Pubby doesn't return HTML with Internet Explorer. 
See "Special Default Behavior for Internet Explorer" part in
http://www.w3.org/TR/swbp-vocab-pub/#negotiation for more explanations.

To support Internet Explorer, we use the Tomcat's valve plumbering.
It allows us to manage content-negociation (instead of Pubby) and make the expected redirection.


2 files have been added in the webapp:

- META-INF/context.xml which declares the Rewrite Valve for the Pubby webapp.
- WEB-INF/rewrite.config which contains the redirection rule, i.e. HTML page for all Mozilla agents.
