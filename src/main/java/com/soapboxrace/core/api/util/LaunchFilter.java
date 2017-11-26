package com.soapboxrace.core.api.util;

import com.soapboxrace.core.bo.AuthenticationBO;
import com.soapboxrace.core.jpa.BanEntity;
import com.soapboxrace.jaxb.login.LoginStatusVO;

import javax.annotation.Priority;
import javax.ejb.EJB;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@LauncherChecks
@Provider
@Priority(Priorities.AUTHORIZATION)
public class LaunchFilter implements ContainerRequestFilter {
	@EJB
	private AuthenticationBO authenticationBO;
	
	public static final DateTimeFormatter banEndFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	@Context
	private HttpServletRequest sr;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String userAgent = requestContext.getHeaderString("User-Agent");
		String hwid = requestContext.getHeaderString("X-HWID");
		String gameLauncherHash = requestContext.getHeaderString("X-GameLauncherHash");
		
		if ((userAgent == null || !userAgent.equals("GameLauncher (+https://github.com/SoapboxRaceWorld/GameLauncher_NFSW)"))
				|| (hwid == null || hwid.trim().isEmpty()) || (gameLauncherHash == null || gameLauncherHash.trim().isEmpty())) {
			LoginStatusVO loginStatusVO = new LoginStatusVO(0L, "", false);
			loginStatusVO.setDescription("Please use MeTonaTOR's launcher. Or, are you tampering?");

			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(loginStatusVO).build());

			return;
		}

		System.out.println(sr.getParameterMap());
		BanEntity banEntity = authenticationBO.checkNonUserBan(hwid, sr.getRemoteAddr(), sr.getParameter("email"));

		if (banEntity != null && banEntity.stillApplies())
		{
			LoginStatusVO loginStatusVO = new BanUtil(banEntity).invoke();

			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity(loginStatusVO).build());
			return;
		}

		System.out.println("user passed launcher + ban check");
	}

	public static class BanUtil
	{
		private BanEntity banEntity;

		public BanUtil(BanEntity banEntity)
		{
			this.banEntity = banEntity;
		}

		public LoginStatusVO invoke()
		{
			LoginStatusVO loginStatusVO = new LoginStatusVO(0L, "", false);
			LoginStatusVO.Ban ban = new LoginStatusVO.Ban();
			ban.setReason(banEntity.getReason());
			ban.setExpires(banEndFormatter.format(banEntity.getEndsAt()));
			
			loginStatusVO.setBan(ban);
//				banDesc += " | Reason: " + banEntity.getReason().trim();
//			}
//
//			if (banEntity.getEndsAt() != null)
//			{
//				banDesc += " | Ends: " + banEndFormatter.format(banEntity.getEndsAt());
//			}

//			loginStatusVO.setDescription(banDesc);
			return loginStatusVO;
		}
	}
}
