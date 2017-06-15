package net.jotorren.microservices.composite.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import net.jotorren.microservices.composite.domain.CompositeData;
import net.jotorren.microservices.composite.domain.CompositeForum;

@Service
public class CompositeService {

	private static final Logger LOG = LoggerFactory.getLogger(CompositeService.class);

	@Value("${content.service.url}")
	private String contentServiceUrl;

	@Value("${forum.service.url}")
	private String forumServiceUrl;

	@Autowired
	private RestTemplate restTemplate;

	private String getIdFromURI(URI uri){
		String[] segments = uri.getPath().split("/");
		return segments[segments.length-1];
	}

	private List<String> saveAllEntities(String contentUrl, String forumUrl, CompositeData data) {
		List<String> newObjectsUris = null;
		
		// first service call
		LOG.info("Step 1: calling [{}]", contentUrl);
		URI contentUriWithTransaction = restTemplate.postForLocation(contentUrl, data);
		LOG.info("Step 1: content created [{}]", contentUriWithTransaction);

		// second service call preparation (using data received from the first one)
		CompositeForum forumData = new CompositeForum();
		forumData.setTopicName(data.getTopicName());
		forumData.setTopicCategory(data.getTopicCategory());
		forumData.setSubjectId(getIdFromURI(contentUriWithTransaction));

		// second service call
		LOG.info("Step 2: calling [{}]", forumUrl);
		URI forumUriWithTransaction = restTemplate.postForLocation(forumUrl, forumData);
		LOG.info("Step 2: forum discussion created [{}]", forumUriWithTransaction);

		// everything seems to be fine
		newObjectsUris = Arrays.asList(contentUriWithTransaction.toString(),
						forumUriWithTransaction.toString());
		
		return newObjectsUris;
	}
	
	public List<String> saveAllEntities(CompositeData data) {
		return saveAllEntities(this.contentServiceUrl, this.forumServiceUrl, data);
	}
	
	public List<String> saveAllEntitiesSuspendingTx(CompositeData data) {
		return saveAllEntities(this.contentServiceUrl + "/noTx", this.forumServiceUrl, data);
	}
	
	public List<String> failForum(CompositeData data) {
		return saveAllEntities(this.contentServiceUrl, this.forumServiceUrl + "/fail", data);
	}
	
	public List<String> timeout(CompositeData data) {
		return saveAllEntities(this.contentServiceUrl + "/timeout", this.forumServiceUrl + "/timeout", data);
	}
}
