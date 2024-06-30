# Transparent-Proxy-Project

This is a project for setting a Transparent Proxy in the network. Transparent Proxy Servers are used to control traffic on a network without the need to configure client devices. Http requests made by the clients will automatically be directed to the Proxy Server by the DNS server and the requests will be handled there. Clients do not know that all their traffic is manipulated.

For this project, I have created 3 Virtual Machines and set them on a virtual NAT network. One machine for Proxy server, one machine for DNS/DHCP server and one machine for the client(could be more but I only needed 1 to test the results). I gave Proxy server machine and the DNS/DHCP server machine static IPs: 10.0.2.2 and 10.0.2.3 respectively. Client machines will automaticallly receive IP addresses from the DHCP server machine.

# Project run-through
1-Client joins the network, receives IP address from the DHCP server (dnsmasq) automatically. 

2-Client tries to send HTTP request to example.com

3-DNS resolve is manipulated by dnsmasq, instead of example.com, dnsmasq resolves the domain name as 10.0.2.2 (Proxy server's machine).

4-Proxy server picks up the request on port 80. 

5-Requests are logged and cached.

6-Server handles and directs the HTTP request to example.com.

7-Server relays the HTTP response to client.

8-Client receives the HTTP response from server, does not know it comes from the server.


# Conclusion

With this setup, we can control traffic from our networks. We can ban certain websites, monitor traffic, etc. Devices on the network do not need to be configured for this to work. All clients automatically follow these steps without even knowing they exist. 
