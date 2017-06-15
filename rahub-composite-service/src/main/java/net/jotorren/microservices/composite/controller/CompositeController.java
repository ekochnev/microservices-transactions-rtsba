package net.jotorren.microservices.composite.controller;

import java.net.URI;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.jotorren.microservices.composite.domain.CompositeData;
import net.jotorren.microservices.composite.service.CompositeService;
import net.jotorren.microservices.rtsba.participant.RtsBaTransactional;

@RestController
public class CompositeController {

	@Autowired
	private CompositeService service;
	
	@RtsBaTransactional
	@RequestMapping(value = "/saveOk", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> saveOk(HttpServletRequest request, @RequestBody CompositeData data) {
		
		List<String> uris = service.saveAllEntities(data);
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+"/"+System.currentTimeMillis()));				
		return new ResponseEntity<List<String>>(uris, headersResp, HttpStatus.CREATED);	
	}

	@RtsBaTransactional
	@RequestMapping(value = "/suspend", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> suspend(HttpServletRequest request, @RequestBody CompositeData data) {
		
		List<String> uris = service.saveAllEntitiesSuspendingTx(data);
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+"/"+System.currentTimeMillis()));				
		return new ResponseEntity<List<String>>(uris, headersResp, HttpStatus.CREATED);	
	}
	
	@RtsBaTransactional
	@RequestMapping(value = "/fail", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> fail(HttpServletRequest request, @RequestBody CompositeData data) {
		
		List<String> uris = service.failForum(data);
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+"/"+System.currentTimeMillis()));				
		return new ResponseEntity<List<String>>(uris, headersResp, HttpStatus.CREATED);	
	}
	
	@RtsBaTransactional(timeout = 3000)
	@RequestMapping(value = "/timeout", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<List<String>> timeout(HttpServletRequest request, @RequestBody CompositeData data) {
		
		List<String> uris = service.timeout(data);
		HttpHeaders headersResp = new HttpHeaders();
		headersResp.setLocation(URI.create(request.getRequestURL().toString()+"/"+System.currentTimeMillis()));				
		return new ResponseEntity<List<String>>(uris, headersResp, HttpStatus.CREATED);	
	}
}
