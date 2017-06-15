package net.jotorren.microservices.rtsba.protocol;

public interface BusinessActivityMessageType {

	long REGISTRATION 		= 100;
	
	long COMPLETED 			= 110;
	long CLOSE 				= 111;	
	long CLOSED 			= 112;
	long COMPENSATE			= 113;
	long COMPENSATED 		= 114;
	long FAIL_COMPENSATING 	= 115;
	
	long CANCEL 			= 120;
	long FAIL_CANCELING 	= 121;
	long CANCELED 			= 122;
	
	long FAIL 				= 130;
	long FAILED 			= 131;

	long EXIT 				= 140;
	long EXITED 			= 141;
	
	long CANNOT_COMPLETE 	= 150;
	long NOT_COMPLETED 		= 151;

}
