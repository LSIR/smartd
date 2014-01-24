package gsn.http;

import gsn.Main;
import gsn.http.ac.User;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

public class MenuServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        PrintWriter out = res.getWriter();
        String selected = req.getParameter("selected");
        out.println("<ul id=\"menu\">");
        out.println("<li" + ("index".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"index.html#home\">home</a></li>");
        out.println("<li" + ("data".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"data.html#data\">data</a></li>");
        out.println("<li" + ("map".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"map.html#map\">map</a></li>");
        out.println("<li" + ("fullmap".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"fullmap.html#fullmap\">fullmap</a></li>");
        out.println("<li" + ("smartd".equals(selected) ? " class=\"selected\"" : "") + "><a href=\"smartd.html#smartd\">smartd</a></li>");
        if (Main.getContainerConfig().isAcEnabled()) {
            out.println("<li><a href=\"/gsn/MyAccessRightsManagementServlet\">access rights management</a></li>");
        }
        out.println("</ul>");
        if (Main.getContainerConfig().isAcEnabled()) {
            out.println("<ul id=\"logintext\">" + displayLogin(req) + "</ul>");
        } else {
            out.println("<ul id=\"linkWebsite\"><li><a href=\"http://gsn.sourceforge.net/\">GSN Home</a></li></ul>");
        }
    }

    private String displayLogin(HttpServletRequest req) {
        String name;
        HttpSession session = req.getSession();
        User user = (User) session.getAttribute("user");
        if (user == null)
            name = "<li><a href=/gsn/MyLoginHandlerServlet> login</a></li>";
        else {
            name = "<li><a href=/gsn/MyLogoutHandlerServlet> logout </a></li>" + "<li><div id=logintextprime >logged in as: " + user.getUserName() + "&nbsp" + "</div></li>";
        }
        return name;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
