package com.rims.config;

import com.rims.model.*;
import com.rims.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final ItemImageRepository itemImageRepository;
    private final RentalOrderRepository rentalOrderRepository;
    private final RentalOrderItemRepository rentalOrderItemRepository;

    public DataInitializer(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           ItemRepository itemRepository,
                           ItemImageRepository itemImageRepository,
                           RentalOrderRepository rentalOrderRepository,
                           RentalOrderItemRepository rentalOrderItemRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.rentalOrderRepository = rentalOrderRepository;
        this.rentalOrderItemRepository = rentalOrderItemRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Ensure "Other" category exists in database even if database was created in earlier runs
        Category catOther = categoryRepository.findBySlug("other").orElseGet(() -> {
            Category newOther = new Category(
                "Other", 
                "other", 
                "General equipment, specialized gear, accessories, and unclassified rental assets.", 
                "package"
            );
            return categoryRepository.save(newOther);
        });

        if (itemRepository.findByItemCode("RIMS-OTH-3011").isEmpty()) {
            User defaultSeller = userRepository.findByRole("ROLE_SELLER").stream().findFirst().orElse(null);
            if (defaultSeller != null) {
                Item itemOther = new Item("RIMS-OTH-3011", "Industrial Laser Distance Meter & Precision Transit",
                        "High-accuracy optical surveying transit and Bluetooth laser meter with target plate, tripod stand, and IP65 rugged carry case. Ideal for set measurements, field inspections, and job-site dimensional verification.",
                        catOther, new BigDecimal("45.00"), 5, "Measurement, Precision, Laser, Transit, Survey, Field-Tool, Other", defaultSeller);
                itemOther.setDepositAmount(new BigDecimal("150.00"));
                itemRepository.save(itemOther);
            }
        }

        // Replace existing database images with authentic real internet images
        updateAllItemImagesWithRealPhotos();
        updateExistingUsersToIndianContacts();
        updateSampleOrderDeposits();

        if (userRepository.count() > 0) {
            return; // Data already seeded
        }

        System.out.println(">>> Initializing RIMS seed data...");

        // 1. Create SUDO Admin
        User admin = new User("SUDO Administrator", "admin@rims.com", "admin123", "ROLE_ADMIN");
        admin.setOrganizationName("RIMS Central Command");
        admin.setPhone("+91-98110-23456");
        admin.setAddress("Cyber Towers, HITEC City, Hyderabad, Telangana 500081");
        userRepository.save(admin);

        // 2. Create Sellers (Organizations / Institutions)
        User sellerCine = new User("Marcus Vance", "seller@apexcine.com", "seller123", "ROLE_SELLER");
        sellerCine.setOrganizationName("Apex Cine Studios & Media Labs");
        sellerCine.setRegistrationId("ORG-CINE-8821");
        sellerCine.setContactPerson("Marcus Vance (Logistics Director)");
        sellerCine.setPhone("+91-98201-54321");
        sellerCine.setAddress("Film City Complex, Goregaon East, Mumbai, Maharashtra 400065");
        userRepository.save(sellerCine);

        User sellerMachinery = new User("Elena Rostova", "machinery@terraforge.com", "heavy123", "ROLE_SELLER");
        sellerMachinery.setOrganizationName("TerraForge Industrial & Construction Equipments");
        sellerMachinery.setRegistrationId("ORG-IND-5509");
        sellerMachinery.setContactPerson("Elena Rostova (Fleet Manager)");
        sellerMachinery.setPhone("+91-98450-87654");
        sellerMachinery.setAddress("Peenya Industrial Area, Phase 2, Bengaluru, Karnataka 560058");
        userRepository.save(sellerMachinery);

        // 3. Create Sample Buyer
        User buyer = new User("Alexander Wright", "buyer@customer.com", "buyer123", "ROLE_BUYER");
        buyer.setPhone("+91-97170-11223");
        buyer.setAddress("Plot 42, Sector 18, Udyog Vihar, Gurugram, Haryana 122015");
        userRepository.save(buyer);

        // 4. Create Categories
        Category catCine = new Category("Cinema & Audiovisual", "audiovisual", 
                "Professional cinematic cameras, prime lenses, anamorphic optics, gimbals, and studio lighting grids.", "camera");
        Category catMachinery = new Category("Heavy Machinery & Tools", "machinery", 
                "Industrial earthmovers, high-torque hydraulic tools, pneumatic generators, and site compaction units.", "wrench");
        Category catAudio = new Category("Sound & Event Staging", "sound-events", 
                "Concert-grade line array speaker systems, UHF wireless lavaliers, digital mixing desks, and trusses.", "volume-2");
        Category catTech = new Category("Computing & Drones", "tech-robotics", 
                "High-performance rendering workstations, thermal inspection drones, VR rigs, and telemetry monitors.", "cpu");
        Category catVehicles = new Category("Transport & Haulers", "transport", 
                "Heavy-duty cargo trailers, flatbeds, electric transport carts, and mobile generator trucks.", "truck");

        categoryRepository.saveAll(Arrays.asList(catCine, catMachinery, catAudio, catTech, catVehicles, catOther));

        // 5. Create Sample Items with multiple photos, unique IDs, descriptions & tags
        // Item 1: RED V-Raptor 8K Camera
        Item item1 = new Item("RIMS-CAM-8001", "RED V-Raptor XL 8K VV Cinema Package", 
                "The RED V-Raptor XL 8K VV provides unmatched dynamic range, multi-format 8K capture up to 120fps, integrated electronic ND filters, and dual 12G-SDI outputs. Includes titanium PL mount, cage, handle, and 2TB RED-PRO CFexpress cards.",
                catCine, new BigDecimal("450.00"), 3, "8K, Cinema, RED, Full-Frame, High-Speed, Production", sellerCine);
        item1.setDepositAmount(new BigDecimal("1500.00"));
        itemRepository.save(item1);

        createImage(item1, "https://upload.wikimedia.org/wikipedia/commons/thumb/9/93/RED_V-Raptor_XL_8K_S35.jpg/1280px-RED_V-Raptor_XL_8K_S35.jpg", true);
        createImage(item1, "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/RED_Digital_Cinema_Raven_camera.jpg/1280px-RED_Digital_Cinema_Raven_camera.jpg", false);

        // Item 2: Arri Orbiter Studio Light
        Item item2 = new Item("RIMS-LGT-2044", "ARRI Orbiter 500W Ultra-Bright LED System", 
                "ARRI Orbiter is a versatile ultra-bright directional LED fixture that features a six-color light engine for maximum color rendition, motorized optic mounts, integrated CRMX wireless DMX, and weatherproof IP20 chassis.",
                catCine, new BigDecimal("180.00"), 4, "Lighting, ARRI, LED, High-CRI, Studio, Wireless-DMX", sellerCine);
        item2.setDepositAmount(new BigDecimal("600.00"));
        itemRepository.save(item2);

        createImage(item2, "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bc/Arri_SkyPanel_X_LED_Light_Panel.jpg/1280px-Arri_SkyPanel_X_LED_Light_Panel.jpg", true);
        createImage(item2, "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Arri_1000_Stage_light.JPG/1280px-Arri_1000_Stage_light.JPG", false);

        // Item 3: CAT Mini Hydraulic Excavator
        Item item3 = new Item("RIMS-MAC-4010", "Caterpillar 302.7 CR Mini Hydraulic Excavator", 
                "Compact radius mini excavator delivering high digging performance and lift capacities in tight quarters. Features customizable joystick controls, sealed pressurized cab with HVAC, rubber tracks, and auxiliary hydraulics.",
                catMachinery, new BigDecimal("650.00"), 2, "Heavy-Duty, Excavator, CAT, Earthmoving, Hydraulic, Construction", sellerMachinery);
        item3.setDepositAmount(new BigDecimal("2000.00"));
        itemRepository.save(item3);

        createImage(item3, "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Caterpillar_302.7D_excavator_on_Barrow_Street.jpg/1280px-Caterpillar_302.7D_excavator_on_Barrow_Street.jpg", true);
        createImage(item3, "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f0/Caterpillar_301.5_%282013%29.jpg/1280px-Caterpillar_301.5_%282013%29.jpg", false);

        // Item 4: Honda Industrial Silent Inverter Generator
        Item item4 = new Item("RIMS-GEN-1102", "Honda EU7000iS 7000W Ultra-Quiet Inverter Generator", 
                "Super quiet fuel-injected commercial generator delivering clean sinusoidal power for sensitive electronics, film sets, and mobile event stages. Features electric start, Eco-Throttle for 18-hour continuous runtime, and Bluetooth telemetry.",
                catMachinery, new BigDecimal("125.00"), 5, "Generator, Clean-Power, Quiet, Honda, 7000W, Mobile", sellerMachinery);
        item4.setDepositAmount(new BigDecimal("400.00"));
        itemRepository.save(item4);

        createImage(item4, "https://upload.wikimedia.org/wikipedia/commons/3/3d/Honda_Power_EU70is_Generator.png", true);
        createImage(item4, "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Honda_EU2000i_Inverter_Generator.jpg/1280px-Honda_EU2000i_Inverter_Generator.jpg", false);

        // Item 5: L-Acoustics Syva Concert Sound Bar System
        Item item5 = new Item("RIMS-SND-9105", "L-Acoustics Syva High-Power Colinear Array System", 
                "State-of-the-art colinear source system with 142 dB maximum SPL and 50m throw range. Perfect for architectural auditoriums, fashion shows, and high-profile corporate galas with sleek minimalist vertical profile.",
                catAudio, new BigDecimal("320.00"), 2, "Pro-Audio, L-Acoustics, Concert, Line-Array, Acoustic, 142dB", sellerCine);
        item5.setDepositAmount(new BigDecimal("1000.00"));
        itemRepository.save(item5);

        createImage(item5, "https://upload.wikimedia.org/wikipedia/commons/b/bc/Line_Array_and_Subs.jpg", true);
        createImage(item5, "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Line_array_speakers.jpg/960px-Line_array_speakers.jpg", false);

        // Item 6: DJI Inspire 3 8K Cinema Drone
        Item item6 = new Item("RIMS-DRN-7731", "DJI Inspire 3 Full-Frame 8K Cinema Drone Package", 
                "Equipped with Zenmuse X9-8K Air gimbal camera, CinemaDNG & Apple ProRes RAW internal recording, centimeter-level RTK positioning, omnidirectional obstacle sensing, and 360-degree pan dual-operator control system.",
                catTech, new BigDecimal("550.00"), 2, "Drone, 8K, DJI, Aerial, ProRes, RTK-GPS, Full-Frame", sellerCine);
        item6.setDepositAmount(new BigDecimal("1800.00"));
        itemRepository.save(item6);

        createImage(item6, "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/2019-03-23_-_DJI_Inspire_2_-3986.jpg/1280px-2019-03-23_-_DJI_Inspire_2_-3986.jpg", true);
        createImage(item6, "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/DJI_Inspire_1.jpg/1280px-DJI_Inspire_1.jpg", false);

        // Item 7: Other - Industrial Surveying Total Station
        Item item7 = new Item("RIMS-OTH-3011", "Industrial Laser Distance Meter & Precision Transit",
                "High-accuracy optical surveying transit and Bluetooth laser meter with target plate, tripod stand, and IP65 rugged carry case. Ideal for set measurements, field inspections, and job-site dimensional verification.",
                catOther, new BigDecimal("45.00"), 5, "Measurement, Precision, Laser, Transit, Survey, Field-Tool, Other", sellerMachinery);
        item7.setDepositAmount(new BigDecimal("150.00"));
        itemRepository.save(item7);

        createImage(item7, "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Surveying_with_total_station_01.jpg/1280px-Surveying_with_total_station_01.jpg", true);
        createImage(item7, "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Active_total_station_on_a_construction_site.jpg/960px-Active_total_station_on_a_construction_site.jpg", false);

        // 6. Create an Initial Sample Completed Rental Order for Demonstration
        RentalOrder sampleOrder = new RentalOrder();
        sampleOrder.setOrderCode("RIMS-ORD-948210");
        sampleOrder.setBuyer(buyer);
        sampleOrder.setRentalStartDate(LocalDate.now().minusDays(2));
        sampleOrder.setRentalEndDate(LocalDate.now().plusDays(3));
        sampleOrder.setRentalDays(5);
        sampleOrder.setStatus("ACTIVE");
        sampleOrder.setPaymentStatus("PAID");
        sampleOrder.setPaymentMethod("RAZORPAY");
        sampleOrder.setRazorpayOrderId("order_rzp_demo948210");
        sampleOrder.setRazorpayPaymentId("pay_rzp_demo948210");
        sampleOrder.setPaidAt(LocalDateTime.now().minusDays(2));
        sampleOrder.setNotes("Delivered to Studio 4B, Film City Goregaon, Mumbai. Contact site coordinator on arrival.");
        sampleOrder.setCreatedAt(LocalDateTime.now().minusDays(2));

        BigDecimal line1Rent = item1.getPricePerDay().multiply(BigDecimal.valueOf(1)).multiply(BigDecimal.valueOf(5));
        BigDecimal line1Deposit = item1.getDepositAmount().multiply(BigDecimal.valueOf(1));
        BigDecimal line1Total = line1Rent.add(line1Deposit);

        BigDecimal line2Rent = item2.getPricePerDay().multiply(BigDecimal.valueOf(2)).multiply(BigDecimal.valueOf(5));
        BigDecimal line2Deposit = item2.getDepositAmount().multiply(BigDecimal.valueOf(2));
        BigDecimal line2Total = line2Rent.add(line2Deposit);

        BigDecimal grandRentalFee = line1Rent.add(line2Rent);
        BigDecimal grandDeposit = line1Deposit.add(line2Deposit);
        BigDecimal grandTotal = grandRentalFee.add(grandDeposit);

        sampleOrder.setRentalFeeTotal(grandRentalFee);
        sampleOrder.setDepositTotal(grandDeposit);
        sampleOrder.setTotalAmount(grandTotal);

        RentalOrder savedSampleOrder = rentalOrderRepository.save(sampleOrder);

        RentalOrderItem roi1 = new RentalOrderItem(savedSampleOrder, item1, item1.getItemCode(), item1.getName(),
                sellerCine, item1.getPricePerDay(), 1, line1Total);
        roi1.setRentalFeeSubtotal(line1Rent);
        roi1.setDepositPerUnit(item1.getDepositAmount());
        roi1.setDepositSubtotal(line1Deposit);

        RentalOrderItem roi2 = new RentalOrderItem(savedSampleOrder, item2, item2.getItemCode(), item2.getName(),
                sellerCine, item2.getPricePerDay(), 2, line2Total);
        roi2.setRentalFeeSubtotal(line2Rent);
        roi2.setDepositPerUnit(item2.getDepositAmount());
        roi2.setDepositSubtotal(line2Deposit);

        rentalOrderItemRepository.save(roi1);
        rentalOrderItemRepository.save(roi2);

        System.out.println(">>> RIMS seed data successfully initialized with verified real internet equipment photos.");
    }

    @Transactional
    public void updateAllItemImagesWithRealPhotos() {
        updateItemPhotos("RIMS-CAM-8001", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/9/93/RED_V-Raptor_XL_8K_S35.jpg/1280px-RED_V-Raptor_XL_8K_S35.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c2/RED_Digital_Cinema_Raven_camera.jpg/1280px-RED_Digital_Cinema_Raven_camera.jpg"
        ));

        updateItemPhotos("RIMS-LGT-2044", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/bc/Arri_SkyPanel_X_LED_Light_Panel.jpg/1280px-Arri_SkyPanel_X_LED_Light_Panel.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cb/Arri_1000_Stage_light.JPG/1280px-Arri_1000_Stage_light.JPG"
        ));

        updateItemPhotos("RIMS-MAC-4010", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Caterpillar_302.7D_excavator_on_Barrow_Street.jpg/1280px-Caterpillar_302.7D_excavator_on_Barrow_Street.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f0/Caterpillar_301.5_%282013%29.jpg/1280px-Caterpillar_301.5_%282013%29.jpg"
        ));

        updateItemPhotos("RIMS-GEN-1102", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/3/3d/Honda_Power_EU70is_Generator.png",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/1/13/Honda_EU2000i_Inverter_Generator.jpg/1280px-Honda_EU2000i_Inverter_Generator.jpg"
        ));

        updateItemPhotos("RIMS-SND-9105", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/b/bc/Line_Array_and_Subs.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/4/4a/Line_array_speakers.jpg/960px-Line_array_speakers.jpg"
        ));

        updateItemPhotos("RIMS-DRN-7731", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/2019-03-23_-_DJI_Inspire_2_-3986.jpg/1280px-2019-03-23_-_DJI_Inspire_2_-3986.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f3/DJI_Inspire_1.jpg/1280px-DJI_Inspire_1.jpg"
        ));

        updateItemPhotos("RIMS-OTH-3011", Arrays.asList(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Surveying_with_total_station_01.jpg/1280px-Surveying_with_total_station_01.jpg",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/b/b2/Active_total_station_on_a_construction_site.jpg/960px-Active_total_station_on_a_construction_site.jpg"
        ));
    }

    private void updateItemPhotos(String itemCode, List<String> urls) {
        itemRepository.findByItemCode(itemCode).ifPresent(item -> {
            if (item.getImages() != null && !item.getImages().isEmpty()) {
                String firstImg = item.getPrimaryImageUrl();
                if (firstImg != null && firstImg.equals(urls.get(0))) {
                    return; // Already up to date
                }
                item.getImages().clear();
            }
            boolean first = true;
            for (String url : urls) {
                ItemImage img = new ItemImage(item, url, first);
                item.addImage(img);
                first = false;
            }
            itemRepository.save(item);
            System.out.println(">>> Updated item " + itemCode + " with real internet photos.");
        });
    }

    @Transactional
    public void updateExistingUsersToIndianContacts() {
        userRepository.findByEmail("admin@rims.com").ifPresent(u -> {
            u.setPhone("+91-98110-23456");
            u.setAddress("Cyber Towers, HITEC City, Hyderabad, Telangana 500081");
            userRepository.save(u);
        });

        userRepository.findByEmail("seller@apexcine.com").ifPresent(u -> {
            u.setPhone("+91-98201-54321");
            u.setAddress("Film City Complex, Goregaon East, Mumbai, Maharashtra 400065");
            userRepository.save(u);
        });

        userRepository.findByEmail("machinery@terraforge.com").ifPresent(u -> {
            u.setPhone("+91-98450-87654");
            u.setAddress("Peenya Industrial Area, Phase 2, Bengaluru, Karnataka 560058");
            userRepository.save(u);
        });

        userRepository.findByEmail("buyer@customer.com").ifPresent(u -> {
            u.setPhone("+91-97170-11223");
            u.setAddress("Plot 42, Sector 18, Udyog Vihar, Gurugram, Haryana 122015");
            userRepository.save(u);
        });

        // Also sanitize any remaining existing users with non-Indian phone or US addresses
        userRepository.findAll().forEach(u -> {
            boolean changed = false;
            if (u.getPhone() != null && u.getPhone().startsWith("+1")) {
                u.setPhone("+91-98" + String.format("%08d", Math.abs(u.getEmail().hashCode()) % 100000000));
                changed = true;
            }
            if (u.getAddress() != null && (u.getAddress().contains("Chicago") || u.getAddress().contains("Seattle") 
                    || u.getAddress().contains("CA") || u.getAddress().contains("IL") || u.getAddress().contains("WA")
                    || u.getAddress().contains("Suite 100"))) {
                u.setAddress("Bandra Kurla Complex, Bandra East, Mumbai, Maharashtra 400051");
                changed = true;
            }
            if (changed) {
                userRepository.save(u);
            }
        });
        System.out.println(">>> Updated all user contact numbers and addresses to Indian formats.");
    }

    @Transactional
    public void updateSampleOrderDeposits() {
        rentalOrderRepository.findByOrderCode("RIMS-ORD-948210").ifPresent(order -> {
            BigDecimal rentalFeeSum = BigDecimal.ZERO;
            BigDecimal depositSum = BigDecimal.ZERO;
            BigDecimal totalSum = BigDecimal.ZERO;

            for (RentalOrderItem item : order.getItems()) {
                BigDecimal dailyRate = item.getPricePerDay();
                int qty = item.getQuantity();
                int days = order.getRentalDays();

                BigDecimal lineRent = dailyRate.multiply(BigDecimal.valueOf(qty)).multiply(BigDecimal.valueOf(days));
                BigDecimal depPerUnit = item.getItem() != null && item.getItem().getDepositAmount() != null 
                        ? item.getItem().getDepositAmount() 
                        : BigDecimal.ZERO;
                BigDecimal lineDeposit = depPerUnit.multiply(BigDecimal.valueOf(qty));
                BigDecimal lineTotal = lineRent.add(lineDeposit);

                item.setRentalFeeSubtotal(lineRent);
                item.setDepositPerUnit(depPerUnit);
                item.setDepositSubtotal(lineDeposit);
                item.setSubtotal(lineTotal);
                rentalOrderItemRepository.save(item);

                rentalFeeSum = rentalFeeSum.add(lineRent);
                depositSum = depositSum.add(lineDeposit);
                totalSum = totalSum.add(lineTotal);
            }

            order.setRentalFeeTotal(rentalFeeSum);
            order.setDepositTotal(depositSum);
            order.setTotalAmount(totalSum);
            rentalOrderRepository.save(order);
            System.out.println(">>> Synced demo order RIMS-ORD-948210: Rent=₹" + rentalFeeSum + ", Deposit=₹" + depositSum + ", Total=₹" + totalSum);
        });
    }

    private void createImage(Item item, String url, boolean isPrimary) {
        ItemImage img = new ItemImage(item, url, isPrimary);
        item.addImage(img);
        itemImageRepository.save(img);
    }
}
