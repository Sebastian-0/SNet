# SNet
This networking library is aimed at making it easier to integrate online-content into programs without having to rewrite the basic communication system each time. The system is based on the client-server architecture and can manage an arbitrary amount of connections simultaneously. The server automatically keeps track of the latency of all the clients and will disconnect them if they don't respond in a given period of time.
<p>
The system will go through a bunch of changes in the future to make it more customizable and efficient. Nevertheless, the current version is stable and works just fine.
</p>
<p>
NOTE: The system currently includes three external dependencies that are not open source at the moment, but they are easy to get rid off. Firstly, there are a bunch of calls to a Debugger which is used to communicate problems that occur within the system, these calls can easily be replaced with other console printouts. Secondly, a method called "closeSilently" is used to close some Closeables without having to worry about exceptions, these calls can be replaced with normal close operations. Lastly, the class Message is based on an object pool to avoid unnecessary object instantiation but this can be remedied by removing the associated code altogether. I intend to get rid of these dependencies in the future. 
</p> 

## License
This networking library is free to use as long as you comply to the GNU GPL v2 license (see LICENSE.txt for details). For clarification, you may compile this library into a jar archive and include it as a dependency in any project of your own, commercial or non-commercial, as long as credit is given to me. Furthermore, I reserve the exclusive right to re-license this library, either for use in specific projects or for public use. 

## Documentation
This may be added at a later date, given that I have time to write it.