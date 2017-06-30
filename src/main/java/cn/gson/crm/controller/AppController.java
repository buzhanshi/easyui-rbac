package cn.gson.crm.controller;

import cn.gson.crm.model.dao.MemberDao;
import cn.gson.crm.model.dao.ResourceDao;
import cn.gson.crm.model.domain.Member;
import cn.gson.crm.model.domain.Resource;
import cn.gson.crm.model.domain.Role;
import cn.gson.crm.model.enums.ResourceType;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;

@Controller
public class AppController {

	@Autowired
	MemberDao memberDao;

	@Autowired
	ResourceDao resourceDao;

	/**
	 * 超级管理员id
	 */
	@Value("${crm.system.super-user-id}")
	Long superUserId;

	@RequestMapping("/")
	public String index() {
		return "index";
	}

	@RequestMapping("/login")
	public String login() {
		return "login";
	}

	@RequestMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}

	/**
	 * 权限resource的js资源
	 * 
	 * @param session
	 * @return
	 */
	@RequestMapping("/resource")
	public String login(HttpSession session, Model model) {
		Object resourceKey = session.getAttribute("resourceKey");
		model.addAttribute("resourceKey", JSONArray.toJSONString(resourceKey));
		return "resource";
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public String doLoin(String userName, String password, RedirectAttributes rAttributes, HttpSession session) {
		// 参数校验
		if (isEmpty(userName) || isEmpty(password)) {
			rAttributes.addFlashAttribute("error", "参数错误！");
			return "redirect:/login";
		}

		Member member = memberDao.findByUserName(userName);

		// 校验密码
		if (member == null || !member.getPassword().equals(DigestUtils.sha256Hex(password)) || !member.getStatus()) {
			rAttributes.addFlashAttribute("error", "用户名或密码错误！");
			return "redirect:/login";
		}

		final List<Resource> allResources;

		// 获取用户可用菜单,所有有权限的请求，所有资源key
		List<Role> roles = member.getRoles();

		List<Resource> menus = new ArrayList<Resource>();
		Set<String> urls = new HashSet<>();
		Set<String> resourceKey = new HashSet<>();
		// 是否是管理员
		session.setAttribute("isSuper", superUserId == member.getId());

		if (superUserId == member.getId()) {
			// 超级管理员，直接获取所有权限资源
			allResources = resourceDao.findByStatus(true, new Sort(Direction.DESC, "weight"));
		} else {
			allResources = new ArrayList<>();
			// forEach 1.8jdk才支持
			roles.forEach(t -> allResources.addAll(t.getResource()));
		}

		allResources.forEach((Resource t) -> {
			// 可用菜单
			if (t.getResType().equals(ResourceType.MENU)) {
				menus.add(t);
			}
			if (isNoneEmpty(t.getMenuUrl())) {
				// 请求资源
				urls.add(t.getMenuUrl());
			}

			//
			String[] funUrls = t.getFunUrls().split(",");
			for (String url : funUrls) {
				if (isNoneEmpty(url)) {
					// 请求资源
					urls.add(url);
				}
			}
			// 资源key
			resourceKey.add(t.getResKey());
		});

		// 将用户可用菜单和权限存入session
		session.setAttribute("menus", menus);
		session.setAttribute("urls", urls);
		session.setAttribute("resourceKey", resourceKey);
		session.setAttribute("s_member", member);

		return "redirect:/";
	}

	/**
	 * 首页
	 * 
	 * @return
	 */
	@RequestMapping("/desktop")
	public String desktop() {
		return "desktop";
	}
	
	/**
	 * 请求权限被拒绝的提醒页面
	 * @return
	 */
	@RequestMapping("/reject")
	public String reject() {
		return "reject";
	}
}
