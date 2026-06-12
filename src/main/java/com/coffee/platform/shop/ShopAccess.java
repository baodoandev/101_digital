package com.coffee.platform.shop;

import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.auth.PrincipalType;
import com.coffee.platform.common.ForbiddenException;
import com.coffee.platform.common.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class ShopAccess {

    private final ShopRepository shopRepo;
    private final OperatorAssignmentRepository assignmentRepo;

    public ShopAccess(ShopRepository shopRepo, OperatorAssignmentRepository assignmentRepo) {
        this.shopRepo = shopRepo;
        this.assignmentRepo = assignmentRepo;
    }

    public Shop loadShopForOwner(Long shopId, AuthPrincipal principal) {
        Shop shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        if (!shop.getOwnerId().equals(principal.id())) {
            throw new ForbiddenException("Not the owner of this shop");
        }
        return shop;
    }

    public Shop loadShopForOperator(Long shopId, AuthPrincipal principal) {
        Shop shop = shopRepo.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        if (principal.type() == PrincipalType.USER && shop.getOwnerId().equals(principal.id())) {
            return shop;
        }
        if (!assignmentRepo.existsByUserIdAndShopId(principal.id(), shopId)) {
            throw new ForbiddenException("Not assigned to this shop");
        }
        return shop;
    }
}
