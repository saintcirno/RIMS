package com.rims.config;

import com.rims.model.RentalCart;
import com.rims.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(true);
        User currentUser = (User) session.getAttribute("currentUser");

        // Ensure cart exists in session
        RentalCart cart = (RentalCart) session.getAttribute("rentalCart");
        if (cart == null) {
            cart = new RentalCart();
            session.setAttribute("rentalCart", cart);
        }

        // Attach to request attribute for easy template rendering
        request.setAttribute("currentUser", currentUser);
        request.setAttribute("rentalCart", cart);

        String path = request.getRequestURI();

        // 1. Admin route protection
        if (path.startsWith("/admin")) {
            if (currentUser == null || !currentUser.isAdmin()) {
                response.sendRedirect("/login/admin?error=restricted");
                return false;
            }
        }

        // 2. Seller route protection
        if (path.startsWith("/seller")) {
            if (currentUser == null || !currentUser.isSeller()) {
                response.sendRedirect("/login/seller?error=restricted");
                return false;
            }
        }

        // 3. Buyer route protection
        if (path.startsWith("/buyer") || path.startsWith("/checkout")) {
            if (currentUser == null || !currentUser.isBuyer()) {
                response.sendRedirect("/login/buyer?error=restricted");
                return false;
            }
        }

        return true;
    }
}
