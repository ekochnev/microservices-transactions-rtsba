package net.jotorren.microservices.rtsba.coordinator.controller;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import net.jotorren.microservices.rtsba.coordinator.saga.CoordinatorSagaService;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextMessageContentType;
import net.jotorren.microservices.rtsba.protocol.CoordinationContextProtocol;

@RestController
@RequestMapping("/rtsba")
public class RtsBaController {

	private static final Logger LOG = LoggerFactory.getLogger(RtsBaController.class);
	private static final String WS_C_COORDINATION_TYPE = "http://docs.oasis-open.org/ws-tx/wsba/2006/06/AtomicOutcome";
	
	@Autowired
	private CoordinationContextProtocol ctp;

	@Autowired
	private CoordinatorSagaService sagas;
	
	@Value("${rtsba.endpoint}")
	private String rtbsEndpoint;
	
	@Value("${rtsba.registration.uri}")
	private String registrationUri;
	
	@RequestMapping(value = "/activation", method = RequestMethod.POST, 
			consumes = CoordinationContextMessageContentType.RTSBA_CONTENT_TYPE)
	public ResponseEntity<CoordinationContext> activate(@RequestBody CreateCoordinationContext data) throws URISyntaxException {
		LOG.info("RTS-BA CRTL :: Trying to open a new coordination context...");

		String txid = ctp.open(data.getExpires());

		CoordinationContext txContext = new CoordinationContext();
		txContext.setCoordinationType(WS_C_COORDINATION_TYPE);
		txContext.setIdentifier(txid);
		
		RegistrationEndpoint registration = new RegistrationEndpoint();
		registration.setMethod(HttpMethod.PUT);
		registration.setContentType(CoordinationContextMessageContentType.RTSBA_REGISTER_CONTENT_TYPE);
		registration.setAddress(rtbsEndpoint + registrationUri + "/" + txContext.getIdentifier());
		txContext.setRegistration(registration);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(new URI(rtbsEndpoint + "/" + txContext.getIdentifier()));
		return new ResponseEntity<CoordinationContext>(txContext, headers, HttpStatus.CREATED);
	}

	@RequestMapping(value = "/registration/{txid}/{sequence}", method = RequestMethod.PUT, 
			consumes = CoordinationContextMessageContentType.RTSBA_REGISTER_CONTENT_TYPE)
	public ResponseEntity<BusinessActivity> register(@PathVariable String txid, @PathVariable String sequence, 
			@RequestBody CoordinationContextParticipant data) throws URISyntaxException {
		LOG.info("RTS-BA CRTL :: Trying to register a new participant inside a coordination context...");

		String activityId = ctp.register(txid, Long.parseLong(sequence), data);
		
		BusinessActivity activity = new BusinessActivity(activityId);
		HttpHeaders headers = new HttpHeaders();
		headers.setLocation(new URI(rtbsEndpoint + "/" + txid + "/" + activityId));
		return new ResponseEntity<BusinessActivity>(activity, headers, HttpStatus.OK);
	}
	
	@RequestMapping(value = "/closed/{txid}", method = RequestMethod.GET)
	public ResponseEntity<Boolean> closed(@PathVariable String txid) {
		LOG.info("RTS-BA CRTL :: Verifying if the coordination context has been closed (for example due a timeout)");
		
		return new ResponseEntity<Boolean>(Boolean.valueOf(sagas.isCompositeTransactionClosed(txid)), null, HttpStatus.OK);
	}
}
