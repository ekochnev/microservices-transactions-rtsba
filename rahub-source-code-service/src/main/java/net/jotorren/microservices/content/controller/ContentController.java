package net.jotorren.microservices.content.controller;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.content.service.ContentService;
import net.jotorren.microservices.rtsba.participant.RtsBaPropagation;
import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessage;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageContentType;
import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;

@RestController
@RequestMapping(value = "/content")
public class ContentController {

	private static final Logger LOG = LoggerFactory.getLogger(ContentController.class);

	@Autowired
	private ContentService service;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public SourceCodeItem get(@PathVariable String id) {
		return this.service.getContent(id);
	}
	
	@RtsBaTransactional(value = RtsBaPropagation.REQUIRED, messages = {RtsBaMessage.CLOSE, RtsBaMessage.COMPENSATE})
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> create(HttpServletRequest request, @RequestBody SourceCodeItem data) {
		LOG.info("Trying to save content...");
		
		String id = this.service.addNewContent(data);

		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+URI.create("/"+id)));				
		return new ResponseEntity<Void>(null, headersResp, HttpStatus.CREATED);	
	}

	@RtsBaTransactional(value = RtsBaPropagation.REQUIRES_NEW)
	@RequestMapping(value = "/noTx", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> createNoTx(HttpServletRequest request, @RequestBody SourceCodeItem data) {
		LOG.info("Trying to save content...");
		
		String id = this.service.addNewContent(data);

		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+URI.create("/"+id)));				
		return new ResponseEntity<Void>(null, headersResp, HttpStatus.CREATED);	
	}

	@RtsBaTransactional(value = RtsBaPropagation.MANDATORY, path = "", messages = {RtsBaMessage.CLOSE, RtsBaMessage.COMPENSATE})
	@RequestMapping(value = "/timeout", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> timeout(HttpServletRequest request, @RequestBody SourceCodeItem data) {
		LOG.info("Trying to save content...");
		
		String id = this.service.addNewContent(data);

		LOG.info("Waiting 1 second...");
		try { Thread.sleep(1000); } catch (InterruptedException e) {}
		
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+URI.create("/"+id)));				
		return new ResponseEntity<Void>(null, headersResp, HttpStatus.CREATED);	
	}
	
	// Protocol methods
	
	@RequestMapping(method = RequestMethod.PUT, consumes = BusinessActivityMessageContentType.CLOSE)
	public void closeCallback(@RequestBody BusinessActivityMessage data) {	
		LOG.info("PARTICIPANT [/content] Protocol :: ON CLOSE");
		this.service.close();
	}	
	
	@RequestMapping(method = RequestMethod.PUT, consumes = BusinessActivityMessageContentType.COMPENSATE)
	public void compensateCallback(@RequestBody BusinessActivityMessage data) {	
		LOG.info("PARTICIPANT [/content] Protocol :: ON COMPENSATE");
		this.service.undo();
	}
}
