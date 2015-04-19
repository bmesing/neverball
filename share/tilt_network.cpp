/**
 *
 * @Copyright Benjamin Mesing 2015
 * 
 * GPL v3+
 **/ 


#include "SDL.h"
#include "SDL_net.h"

#include <iostream>
#include <string>
#include <fstream>
#include <sstream>


extern "C" {
#include "config.h"
#include "tilt.h"
}



#define LISTENING_PORT 35462

static TCPsocket serverSocket;
static TCPsocket dataSocket = NULL;
static SDLNet_SocketSet socketSet;
/** Holds the last tiltX data received via network. */
static float tiltX = 0;
/** Holds the last tiltZ data received via network. */
static float tiltZ = 0;

void startServer() {
	IPaddress* ip = new IPaddress();
	SDLNet_ResolveHost(ip, NULL, LISTENING_PORT);
	printf("Server IP: %x, %d\n", ip->host, ip->port);
	socketSet = SDLNet_AllocSocketSet(2);
	serverSocket = SDLNet_TCP_Open(ip);
	if (serverSocket != NULL) {
		SDLNet_TCP_AddSocket(socketSet, serverSocket);
		std::cout << "Opened socket for listening" << std::endl;
	} else {
		std::cerr << "Could not open socket" << std::cerr ;
	}
	
}

void closeDataSocket() {
	printf("Closing connection\n");
	SDLNet_TCP_DelSocket(socketSet, dataSocket);
	SDLNet_TCP_Close(dataSocket);
	dataSocket = NULL;
}

void openDataSocket() {
	if (dataSocket != NULL) {
		closeDataSocket();
	}
	dataSocket = SDLNet_TCP_Accept(serverSocket);
	if ( dataSocket != NULL ) {
		std::cout << "Connection received" << std::endl;
		SDLNet_TCP_AddSocket(socketSet, dataSocket);
	} else {
		std::cerr << "Could not open data socket" << std::endl;
	}
}
	

	
void readData() {
	// read x and z, might cause trouble, if more than one data package is in 
	// the buffer
	char data[31];
	int amount = SDLNet_TCP_Recv(dataSocket, data, 30);
	if (amount > 0)  {
		data[amount] = '\0';
		std::string message(data);
		std::cout << message << std::endl;
		std::istringstream messageReader(message);
		messageReader >> tiltX >> tiltZ;
		//std::cout << "tilt X: " << tiltX << ", tiltZ: " << tiltZ << std::endl;
	} else {	// connection was lost
		closeDataSocket();
	}

}

void initSDLNet() {
	/* Initialize the network */
	if ( SDLNet_Init() < 0 ) {
		std::cerr <<  "Couldn't initialize SDL net: " << SDLNet_GetError()) << std::endl;
		SDL_Quit();
		exit(1);
	}
}

void handleNetwork() {
	/* Wait for events */
	if (SDLNet_CheckSockets(socketSet, 0) > 0) {
		// std::cout << "Data available" << std::endl;
		/* Check for new connections */
		while ( SDLNet_SocketReady(serverSocket) ) {
			openDataSocket();
		}
		while (SDLNet_SocketReady(dataSocket) ) {
			readData();
		}
	}
}

	
extern "C" {


void tilt_init(void) {
	initSDLNet();
	startServer();
}

void tilt_free(void) {
}

int  tilt_stat(void) {
	return 1;
}

int  tilt_get_button(int *, int *) {
	return 0;
}


float tilt_get_x(void) {
	handleNetwork();
	return tiltX;
}

float tilt_get_z(void) {
	return tiltZ;
}


} // extern C