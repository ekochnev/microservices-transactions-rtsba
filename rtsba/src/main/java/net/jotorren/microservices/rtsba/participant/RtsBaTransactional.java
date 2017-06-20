package net.jotorren.microservices.rtsba.participant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;

@Inherited
@Target({ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface RtsBaTransactional {

	RtsBaPropagation value() default RtsBaPropagation.REQUIRED; 
	RtsBaMessage[] messages() default RtsBaMessage.NONE;
	String path() default "";
	long timeout() default -1;
	boolean strict() default true;
}
