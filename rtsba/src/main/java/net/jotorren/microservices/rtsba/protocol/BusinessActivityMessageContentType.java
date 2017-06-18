package net.jotorren.microservices.rtsba.protocol;

public interface BusinessActivityMessageContentType {

	String CLOSE 			= "application/vnd.rtsba.close+json";
	String COMPENSATE 		= "application/vnd.rtsba.compensate+json";
	String FAILED 			= "application/vnd.rtsba.failed+json";
	String CANCEL 			= "application/vnd.rtsba.cancel+json";
	String EXITED 			= "application/vnd.rtsba.exited+json";
	String NOT_COMPLETED 	= "application/vnd.rtsba.not-completed+json";
}
