package net.jotorren.microservices.content.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.jotorren.microservices.content.dao.ContentDao;
import net.jotorren.microservices.content.domain.SourceCodeItem;
import net.jotorren.microservices.rtsba.participant.RtsBaDataHolder;

@Service
public class ContentService {

	private static final Logger LOG = LoggerFactory.getLogger(ContentService.class);

	@Autowired
	private RtsBaDataHolder dataHolder;
	
	@Autowired
	private ContentDao dao;
	
	public String addNewContent(SourceCodeItem content) {
		String uuid = UUID.randomUUID().toString();
		content.setItemId(uuid);
		
		LOG.info("Calling DAO to save {}", uuid);
		SourceCodeItem saved = dao.save(content);
		dataHolder.put("newContentId", saved.getItemId());
		
		return saved.getItemId();
	}
	
	public SourceCodeItem getContent(String pk) {
		return dao.findOne(pk);
	}

	public void close() {
		dataHolder.delete("newContentId");
	}
	
	public void undo() {
		String pk = (String)dataHolder.get("newContentId");
		dao.delete(pk);
		dataHolder.delete("newContentId");
	}
}
