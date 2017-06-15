package net.jotorren.microservices.forum.controller;

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

import net.jotorren.microservices.forum.domain.Forum;
import net.jotorren.microservices.forum.service.ForumService;
import net.jotorren.microservices.rtsba.participant.RtsBaPropagation;
import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessage;
import net.jotorren.microservices.rtsba.protocol.BusinessActivityMessageContentType;
import net.jotorren.microservices.rtsba.protocol.RtsBaMessage;

@RestController
@RequestMapping(value = "/forum")
public class ForumController {

	private static final Logger LOG = LoggerFactory.getLogger(ForumController.class);

	@Autowired
	private ForumService service;

	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Forum get(@PathVariable String id) {
		return this.service.getForum(id);
	}
	
	@RtsBaTransactional(value = RtsBaPropagation.MANDATORY, messages = {RtsBaMessage.CLOSE, RtsBaMessage.COMPENSATE, RtsBaMessage.FAILED})
	@RequestMapping(method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> create(HttpServletRequest request, @RequestBody Forum data) {
		LOG.info("Trying to save forum...");
		
		String id = this.service.addNewForum(data);

		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+URI.create("/"+id)));				
		return new ResponseEntity<Void>(null, headersResp, HttpStatus.CREATED);	
	}

	@RtsBaTransactional(value = RtsBaPropagation.MANDATORY, messages = {RtsBaMessage.CLOSE, RtsBaMessage.COMPENSATE, RtsBaMessage.FAILED})
	@RequestMapping(value = "/fail", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> fail(HttpServletRequest request, @RequestBody Forum data) {
		LOG.info("Trying to save forum...");
		
		String id = this.service.forceFail(data);

		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+URI.create("/"+id)));				
		return new ResponseEntity<Void>(null, headersResp, HttpStatus.CREATED);	
	}
	
	@RtsBaTransactional(value = RtsBaPropagation.MANDATORY, messages = {RtsBaMessage.CLOSE, RtsBaMessage.COMPENSATE, RtsBaMessage.FAILED})
	@RequestMapping(value = "/timeout", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Void> timeout(HttpServletRequest request, @RequestBody Forum data) {
		LOG.info("Trying to save forum...");
		
		String id = this.service.addNewForum(data);

		LOG.info("Waiting 4 seconds...");
		try { Thread.sleep(4000); } catch (InterruptedException e) {}
		
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+URI.create("/"+id)));				
		return new ResponseEntity<Void>(null, headersResp, HttpStatus.CREATED);	
	}
	
	// Protocol methods

	@RequestMapping(method = RequestMethod.PUT, consumes = BusinessActivityMessageContentType.CLOSE)
	public void closeCallback(@RequestBody BusinessActivityMessage data) {	
		LOG.info("PARTICIPANT [/forum] Protocol :: ON CLOSE");
		this.service.close();
	}	

	@RequestMapping(value = "/fail", method = RequestMethod.PUT, consumes = BusinessActivityMessageContentType.FAILED)
	public void failedCallback(@RequestBody BusinessActivityMessage data) {	
		LOG.info("PARTICIPANT [/forum] Protocol :: ON FAILED");
		this.service.failed();
	}
	
	@RequestMapping(method = RequestMethod.PUT, consumes = BusinessActivityMessageContentType.COMPENSATE)
	public void compensateCallback(@RequestBody BusinessActivityMessage data) {	
		LOG.info("PARTICIPANT [/forum] Protocol :: ON COMPENSATE");
		this.service.undo();
	}
	
	@RequestMapping(value="/timeout", method = RequestMethod.PUT, consumes = BusinessActivityMessageContentType.COMPENSATE)
	public void compensateTimeoutCallback(@RequestBody BusinessActivityMessage data) {	
		LOG.info("PARTICIPANT [/forum] Protocol :: ON COMPENSATE");
		this.service.undo();
	}
}
