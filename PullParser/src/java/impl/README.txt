This is default XPP2 implementation.

factory		- creation of classes implementing XPP2 API
format		- XML output 
node		- creation of regular and pull XML trees
pullparser	- implementation of pull parsing (tokenizer + parser)
small		- special verion of factory that only uses pullparser and tag
		  (especially well suited for small footprint such as J2ME env)
tag		- repsenting end/start tag and some other base classes (used in impl)
