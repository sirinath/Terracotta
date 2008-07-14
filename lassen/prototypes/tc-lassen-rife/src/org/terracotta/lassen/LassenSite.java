package org.terracotta.lassen;

import org.terracotta.lassen.elements.EditUser;
import org.terracotta.lassen.elements.ListUsers;
import org.terracotta.lassen.elements.RegisterUser;
import org.terracotta.lassen.elements.Welcome;
import org.terracotta.lassen.services.impl.DefaultUserService;

import com.uwyn.rife.engine.Site;
import com.uwyn.rife.engine.SiteBuilder;
import com.uwyn.rife.ioc.PropertyValueTemplate;
import com.uwyn.rife.rep.BlockingParticipant;

public class LassenSite extends BlockingParticipant {
    private Site site;

    @Override
	protected void initialize() {
    	site = new SiteBuilder("main", getResourceFinder())
    			.addProperty("userService", new DefaultUserService())
    			
    			.setArrival("Welcome")
    			
    			.addGlobalExit("index", "Welcome")
    			.addGlobalExit("register", "RegisterUser")
    			.addGlobalExit("listusers", "ListUsers")
    			
    			.enterElement()
    				.setImplementation(Welcome.class)
    				.addProperty("template", new PropertyValueTemplate("welcome"))
    			.leaveElement()
    			
    			.enterElement()
    				.setImplementation(RegisterUser.class)
    				.addProperty("template", new PropertyValueTemplate("registration"))
    			.leaveElement()
    			
    			.enterElement()
    				.setImplementation(ListUsers.class)
    				.addProperty("template", new PropertyValueTemplate("listusers"))
    			.leaveElement()
    			
    			.enterElement()
    				.setImplementation(EditUser.class)
    				.addProperty("template", new PropertyValueTemplate("edituser"))
    			.leaveElement()
    		.getSite();
	}

	@Override
	protected Object _getObject() {
		return site;
	}
}