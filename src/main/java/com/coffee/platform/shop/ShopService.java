package com.coffee.platform.shop;

import com.coffee.platform.auth.AppUser;
import com.coffee.platform.auth.AppUserRepository;
import com.coffee.platform.auth.AuthPrincipal;
import com.coffee.platform.auth.PrincipalType;
import com.coffee.platform.auth.Role;
import com.coffee.platform.common.ConflictException;
import com.coffee.platform.common.ForbiddenException;
import com.coffee.platform.common.NotFoundException;
import com.coffee.platform.shop.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ShopService {

    private final ShopRepository shopRepo;
    private final MenuItemRepository menuItemRepo;
    private final ShopQueueRepository shopQueueRepo;
    private final OperatorAssignmentRepository assignmentRepo;
    private final AppUserRepository userRepo;
    private final ShopAccess shopAccess;

    public ShopService(ShopRepository shopRepo,
                       MenuItemRepository menuItemRepo,
                       ShopQueueRepository shopQueueRepo,
                       OperatorAssignmentRepository assignmentRepo,
                       AppUserRepository userRepo,
                       ShopAccess shopAccess) {
        this.shopRepo = shopRepo;
        this.menuItemRepo = menuItemRepo;
        this.shopQueueRepo = shopQueueRepo;
        this.assignmentRepo = assignmentRepo;
        this.userRepo = userRepo;
        this.shopAccess = shopAccess;
    }

    @Transactional
    public ShopResponse createShop(CreateShopRequest req, AuthPrincipal principal) {
        requireOwner(principal);

        Shop shop = new Shop();
        shop.setOwnerId(principal.id());
        shop.setName(req.name());
        shop.setContactPhone(req.contactPhone());
        shop.setContactEmail(req.contactEmail());
        shop.setAddressLine(req.addressLine());
        shop.setCity(req.city());
        shop.setCountry(req.country());
        shop.setLatitude(req.latitude());
        shop.setLongitude(req.longitude());
        shop.setTimezone(req.timezone());
        shop.setCurrency(req.currency());
        shop.setOpeningTime(LocalTime.parse(req.openingTime()));
        shop.setClosingTime(LocalTime.parse(req.closingTime()));
        if (req.avgServiceSeconds() != null) {
            shop.setAvgServiceSeconds(req.avgServiceSeconds());
        }

        shop = shopRepo.save(shop);
        return ShopResponse.from(shop);
    }

    public ShopResponse getShop(Long id) {
        Shop shop = shopRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Shop not found"));
        return ShopResponse.from(shop);
    }

    public List<ShopResponse> listShops() {
        return shopRepo.findByStatus("ACTIVE").stream()
                .map(ShopResponse::from)
                .toList();
    }

    public List<ShopResponse> findNearest(double lat, double lng, double radiusKm, int limit) {
        double radiusMeters = radiusKm * 1000.0;
        return shopRepo.findNearest(lat, lng, radiusMeters, limit).stream()
                .map(ShopResponse::from)
                .toList();
    }

    @Transactional
    public MenuItemResponse addMenuItem(Long shopId, CreateMenuItemRequest req, AuthPrincipal principal) {
        requireOwner(principal);
        shopAccess.loadShopForOwner(shopId, principal);

        if (menuItemRepo.existsByShopIdAndName(shopId, req.name())) {
            throw new ConflictException("DUPLICATE_MENU_ITEM", "Menu item name already exists");
        }

        MenuItem item = new MenuItem();
        item.setShopId(shopId);
        item.setName(req.name());
        item.setDescription(req.description());
        item.setPriceMinor(req.priceMinor());

        item = menuItemRepo.save(item);
        return MenuItemResponse.from(item);
    }

    public List<MenuItemResponse> listMenuItems(Long shopId) {
        shopRepo.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        return menuItemRepo.findByShopIdAndAvailableTrue(shopId).stream()
                .map(MenuItemResponse::from)
                .toList();
    }

    @Transactional
    public QueueResponse addQueue(Long shopId, CreateQueueRequest req, AuthPrincipal principal) {
        requireOwner(principal);
        shopAccess.loadShopForOwner(shopId, principal);

        List<ShopQueue> existing = shopQueueRepo.findByShopId(shopId);
        if (existing.size() >= 3) {
            throw new ConflictException("MAX_QUEUES_REACHED", "Shop already has 3 queues");
        }

        Set<Integer> used = existing.stream()
                .map(ShopQueue::getPositionIndex)
                .collect(Collectors.toSet());
        int nextIndex = 0;
        while (used.contains(nextIndex)) {
            nextIndex++;
        }

        ShopQueue queue = new ShopQueue();
        queue.setShopId(shopId);
        queue.setLabel(req.label());
        queue.setMaxSize(req.maxSize());
        queue.setPositionIndex(nextIndex);

        queue = shopQueueRepo.save(queue);
        return QueueResponse.from(queue);
    }

    public List<QueueResponse> listQueues(Long shopId) {
        shopRepo.findById(shopId)
                .orElseThrow(() -> new NotFoundException("Shop not found"));

        return shopQueueRepo.findByShopId(shopId).stream()
                .map(QueueResponse::from)
                .toList();
    }

    @Transactional
    public void assignOperator(Long shopId, AssignOperatorRequest req, AuthPrincipal principal) {
        requireOwner(principal);
        shopAccess.loadShopForOwner(shopId, principal);

        AppUser targetUser = userRepo.findById(req.userId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (targetUser.getRole() != Role.OPERATOR) {
            throw new ForbiddenException("User is not an operator");
        }

        if (assignmentRepo.existsByUserIdAndShopId(req.userId(), shopId)) {
            throw new ConflictException("ALREADY_ASSIGNED", "Operator already assigned to this shop");
        }

        assignmentRepo.save(new OperatorAssignment(req.userId(), shopId));
    }

    private void requireOwner(AuthPrincipal principal) {
        if (principal.type() != PrincipalType.USER || !"OWNER".equals(principal.role())) {
            throw new ForbiddenException("Only shop owners can perform this action");
        }
    }
}
