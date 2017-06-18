package net.jotorren.microservices.rtsba;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rtsba.amqp")
public class RtsBaProtocolProperties {

//	@Value("${rtsba.amqp.generateDDL:true}")
	private boolean generateDDL = true;
	
//	@Value("${rtsba.amqp.exchange-name:Axon.EventBus}")
	private String exchangeName = "Axon.EventBus";
	
//	@Value("${rtsba.amqp.routing-key:Axon.Event}")
	private String routingKey = "Axon.Event";
	
//	@Value("${rtsba.amqp.durable-queue:false}")
	private boolean durableQueue = false;

	public boolean isGenerateDDL() {
		return generateDDL;
	}

	public void setGenerateDDL(boolean generateDDL) {
		this.generateDDL = generateDDL;
	}

	public String getExchangeName() {
		return exchangeName;
	}

	public void setExchangeName(String exchangeName) {
		this.exchangeName = exchangeName;
	}

	public String getRoutingKey() {
		return routingKey;
	}

	public void setRoutingKey(String routingKey) {
		this.routingKey = routingKey;
	}

	public boolean isDurableQueue() {
		return durableQueue;
	}

	public void setDurableQueue(boolean durableQueue) {
		this.durableQueue = durableQueue;
	}

}
