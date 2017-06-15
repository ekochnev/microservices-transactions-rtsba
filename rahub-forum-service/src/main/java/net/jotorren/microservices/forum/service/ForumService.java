package net.jotorren.microservices.forum.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.jotorren.microservices.forum.dao.ForumDao;
import net.jotorren.microservices.forum.domain.Forum;
import net.jotorren.microservices.rtsba.participant.RtsBaDataHolder;

@Service
public class ForumService {

	private static final Logger LOG = LoggerFactory.getLogger(ForumService.class);

	@Autowired
	private RtsBaDataHolder dataHolder;
	
	@Autowired
	private ForumDao dao;
	
	public String addNewForum(Forum Forum) {
		String uuid = UUID.randomUUID().toString();
		Forum.setForumId(uuid);
		
		LOG.info("Calling DAO to save {}", uuid);
		Forum saved = dao.save(Forum);
		dataHolder.put("newForumId", saved.getForumId());
		
		return saved.getForumId();
	}
	
	public Forum getForum(String pk) {
		return dao.findOne(pk);
	}

	public String forceFail(Forum Forum) {
		try { Thread.sleep(1000); } catch (InterruptedException e) {}
		throw new UnsupportedOperationException("Backend unavailable"); 
	}
	
	public void close() {
		dataHolder.delete("newForumId");
	}

	public void failed() {
	}
	
	public void undo() {
		String pk = (String)dataHolder.get("newForumId");
		dao.delete(pk);
		dataHolder.delete("newForumId");
	}
}
