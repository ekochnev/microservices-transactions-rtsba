package net.jotorren.microservices.rtsba.coordinator.controller;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.jotorren.microservices.rtsba.BusinessActivity;
import net.jotorren.microservices.rtsba.CoordinationContext;
import net.jotorren.microservices.rtsba.CoordinationContextParticipant;
import net.jotorren.microservices.rtsba.CreateCoordinationContext;
import net.jotorren.microservices.rtsba.RegistrationEndpoint;
import net.jotorren.microservices.rtsba.RtsBaProperties;
import net.jotorren.microservices.rtsba.coordinator.saga.CoordinatorSagaService;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextMessageContentType;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextProtocol;

@RestController
@RequestMapping("/rtsba")
public class RtsBaController {

	private static final Logger LOG = LoggerFactory.getLogger(RtsBaController.class);
	private static final String WS_C_COORDINATION_TYPE = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/AtomicOutcome";

	@Autowired
	private RtsBaProperties configuration;
	
	@Autowired
	private CoordinationContextProtocol ctp;

	@Autowired
	private CoordinatorSagaService sagas;
	
	@RequestMapping(value = "/activation", method = RequestMethod.POST, 
			consumes = CoordinationContextMessageContentType.RTSBA_CONTENT_TYPE)
	public ResponseEntity<CoordinationContext> activate(@RequestBody CreateCoordinationContext data) throws URISyntaxException {
		LOG.info("RTS-BA CRTL :: Opening a new coordination context...");

		String coordCtxId = ctp.open(data.getExpires());

		CoordinationContext coordCtx = new CoordinationContext();
		coordCtx.setCoordinationType(WS_C_COORDINATION_TYPE);
		coordCtx.setIdentifier(coordCtxId);
		
		RegistrationEndpoint registration = new RegistrationEndpoint();
		registration.setMethod(HttpMethod.PUT);
		registration.setContentType(CoordinationContextMessageContentType.RTSBA_REGISTER_CONTENT_TYPE);
		registration.setAddress(this.configuration.getEndpoint() + this.configuration.getRegistrationUri() + "/" + coordCtx.getIdentifier());
		coordCtx.setRegistration(registration);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(new URI(this.configuration.getEndpoint() + "/" + coordCtx.getIdentifier()));
		return new ResponseEntity<CoordinationContext>(coordCtx, headers, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/registration/{coordCtxId}/{sequence}", method = RequestMethod.PUT, 
			consumes = CoordinationContextMessageContentType.RTSBA_REGISTER_CONTENT_TYPE)
	public ResponseEntity<BusinessActivity> register(@PathVariable String coordCtxId, @PathVariable String sequence, 
			@RequestBody CoordinationContextParticipant data) throws URISyntaxException {
		LOG.info("RTS-BA CRTL :: Registering a new participant inside coordination context...");

		String activityId = ctp.register(coordCtxId, Long.parseLong(sequence), data);
		
		BusinessActivity activity = new BusinessActivity(activityId);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(new URI(this.configuration.getEndpoint() + "/" + coordCtxId + "/" + activityId));
		return new ResponseEntity<BusinessActivity>(activity, headers, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/closed/{coordCtxId}", method = RequestMethod.GET)
	public ResponseEntity<Boolean> closed(@PathVariable String coordCtxId) {
		LOG.info("RTS-BA CRTL :: Verifying coordination context status {}", coordCtxId);
		
		return new ResponseEntity<Boolean>(Boolean.valueOf(sagas.isCoordinationContextClosed(coordCtxId)), null, HttpStatus.OK);
	}
}
