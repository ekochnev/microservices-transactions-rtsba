package net.jotorren.microservices.rtsba.participant;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import net.jotorren.microservices.context.ThreadLocalContext;
import net.jotorren.microservices.rtsba.participant.aop.RtsBaClient;

@Component
public class RtsBaDataHolder {

	@Autowired
	private RedisTemplate<String, Object> redisTemplate;
	
	private HashOperations<String, String, Object> hashOps;
	
    @PostConstruct
    private void init() {
        this.hashOps = this.redisTemplate.opsForHash();
    }
    
	public void put(String key, Object value) {
		String txid = ThreadLocalContext.get(RtsBaClient.RTSBA_TRANSACTION_ID, String.class);
		this.hashOps.put(txid, key, value);
	}

	public Object get(String key) {
		String txid = ThreadLocalContext.get(RtsBaClient.RTSBA_TRANSACTION_ID, String.class);
		return this.hashOps.get(txid, key);
	}

	public void delete(String key) {
		String txid = ThreadLocalContext.get(RtsBaClient.RTSBA_TRANSACTION_ID, String.class);
		this.hashOps.delete(txid, key);
	}
}
