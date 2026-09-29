/**
 * Shared seed data and seeding logic, used by both seedEmulator.ts (local dev) and
 * seedProduction.ts (the real `mackson-delivery` project). Kept in one place so the two entry
 * points can't drift apart — they differ only in which Firebase project they talk to and what
 * safety checks guard them.
 */
import type { Auth } from "firebase-admin/auth";
import type { Firestore } from "firebase-admin/firestore";

export const STORE_ID = "store-estcourt";
// Mackson's is a single-branch business — this was originally seeded under a placeholder
// "store-sandton" ID before the client's real branch address was confirmed. Kept here so
// seedCatalogueAndStaff can clean up the stale document instead of leaving orphaned data behind.
const LEGACY_STORE_ID = "store-sandton";

// Prices, brand names, and barcodes below are taken directly from the client's own price list
// (PRICE LIST.xls, ~67,700 SKUs across the whole store) — this is the real Mackson's catalogue,
// not invented placeholder data. A handful of items (the two MACKSONS-brand loaves) are the
// client's own in-house bakery line. Photos are real, freely-licensed (Wikimedia Commons)
// product photography rather than icon placeholders, chosen to be generic/unbranded where
// possible; a few (the egg carton, the peanut butter jar, the wafer bars) show a real
// third-party brand because that was the cleanest available photo — fine for a non-commercial
// student prototype, but swap in your own product photography (via Cloud Storage, per Part 1's
// architecture) before any real-world use.
//
// The products from "Snowflake Cake Wheat Flour" through "Imana Beef & Onion Flavoured Soup"
// (below) use actual product photos supplied by the client, hosted as static files under
// admin-web/public/product-images/. Most prices/barcodes for these were matched against the
// client's price list too; where no match existed, the description says so explicitly and the
// barcode is a placeholder (0000000000001 upward).
const bread = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Fresh_made_bread_05.jpg/500px-Fresh_made_bread_05.jpg";
const milk = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a5/Glass_of_Milk_%2833657535532%29.jpg/500px-Glass_of_Milk_%2833657535532%29.jpg";
const eggs = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/22/2020-05-05_18_20_27_A_carton_of_a_dozen_Large_Grade_A_Chicken_Eggs_from_Egg-land%27s_Best_in_the_Franklin_Farm_section_of_Oak_Hill%2C_Fairfax_County%2C_Virginia.jpg/500px-2020-05-05_18_20_27_A_carton_of_a_dozen_Large_Grade_A_Chicken_Eggs_from_Egg-land%27s_Best_in_the_Franklin_Farm_section_of_Oak_Hill%2C_Fairfax_County%2C_Virginia.jpg";
const bananas = "https://upload.wikimedia.org/wikipedia/commons/d/de/Bananavarieties.jpg";
const tomatoes = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/Tomato_je.jpg/500px-Tomato_je.jpg";
const chicken = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/db/CHICKEN_BREAST.jpg/500px-CHICKEN_BREAST.jpg";
const pasta = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/92/Spaghettoni.jpg/500px-Spaghettoni.jpg";
const cola = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/23/Glass_of_Cola.jpg/500px-Glass_of_Cola.jpg";
const apple = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a6/Pink_lady_and_cross_section.jpg/500px-Pink_lady_and_cross_section.jpg";
const potato = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/Patates.jpg/500px-Patates.jpg";
const onion = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Mixed_onions.jpg/500px-Mixed_onions.jpg";
const carrot = "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a2/Vegetable-Carrot-Bundle-wStalks.jpg/500px-Vegetable-Carrot-Bundle-wStalks.jpg";
const butternut = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/77/Cucurbita_moschata_Butternut_2012_G2.jpg/500px-Cucurbita_moschata_Butternut_2012_G2.jpg";
const fish = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/cd/Solea_solea_1.jpg/500px-Solea_solea_1.jpg";
const peanutButter = "https://upload.wikimedia.org/wikipedia/commons/thumb/1/11/2020-03-24_20_57_22_An_open_jar_of_Skippy_Creamy_Peanut_Butter_in_the_Dulles_section_of_Sterling%2C_Loudoun_County%2C_Virginia.jpg/500px-2020-03-24_20_57_22_An_open_jar_of_Skippy_Creamy_Peanut_Butter_in_the_Dulles_section_of_Sterling%2C_Loudoun_County%2C_Virginia.jpg";
const mayo = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/60/Mayonnaise_%281%29.jpg/500px-Mayonnaise_%281%29.jpg";
const sugar = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3c/Sucre_blanc_cassonade_complet_rapadura.jpg/500px-Sucre_blanc_cassonade_complet_rapadura.jpg";
const oil = "https://upload.wikimedia.org/wikipedia/commons/thumb/3/33/Bottle_1_liter_Sunflower_refined_oil.jpg/500px-Bottle_1_liter_Sunflower_refined_oil.jpg";
const teaBags = "https://upload.wikimedia.org/wikipedia/commons/thumb/8/80/Tea_bags.jpg/500px-Tea_bags.jpg";
const beans = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0b/Baked_beans_in_tomato_sauce.jpg/500px-Baked_beans_in_tomato_sauce.jpg";
const chips = "https://upload.wikimedia.org/wikipedia/commons/thumb/6/69/Potato-Chips.jpg/500px-Potato-Chips.jpg";

// Distinct stand-ins for products that used to share one generic photo with an unrelated item
// (e.g. the milk glass was also standing in for a strawberry yoghurt) — still not the real
// branded packaging, but at least no two different products render an identical picture. See
// the "give me a list" thread: the client is sourcing real photos for these to replace them.
const breadLoafPlain = "https://upload.wikimedia.org/wikipedia/commons/thumb/2/21/Bread_%2813805384963%29.jpg/500px-Bread_%2813805384963%29.jpg";
const breadBrown1 = "https://upload.wikimedia.org/wikipedia/commons/thumb/9/98/Brown_Bread.jpg/500px-Brown_Bread.jpg";
const breadBrown2 = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/79/Vegan_Nine_Grain_Whole_Wheat_Bread.jpg/500px-Vegan_Nine_Grain_Whole_Wheat_Bread.jpg";
const yoghurtStrawberry = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/48/2020-02-19_16_27_01_A_cup_of_Chobani_Greek_Yogurt_with_Strawberry_on_the_Bottom_while_being_mixed_in_the_Franklin_Farm_section_of_Oak_Hill%2C_Fairfax_County%2C_Virginia.jpg/500px-2020-02-19_16_27_01_A_cup_of_Chobani_Greek_Yogurt_with_Strawberry_on_the_Bottom_while_being_mixed_in_the_Franklin_Farm_section_of_Oak_Hill%2C_Fairfax_County%2C_Virginia.jpg";
const riceSack1 = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dc/Basmati_Rice_Sack_%281%29.jpg/500px-Basmati_Rice_Sack_%281%29.jpg";
const riceSack2 = "https://upload.wikimedia.org/wikipedia/commons/thumb/5/5a/AAA_Golden_Boy_Fragrant_Rice_Sack_%281%29.jpg/500px-AAA_Golden_Boy_Fragrant_Rice_Sack_%281%29.jpg";
const spaghetti2 = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/da/Spaghetti_%28247838199%29.jpeg/500px-Spaghetti_%28247838199%29.jpeg";
const irnBru = "https://upload.wikimedia.org/wikipedia/commons/thumb/0/0f/IrnBru500ml.jpg/500px-IrnBru500ml.jpg";
const chipsBag2 = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d2/Zimba_chips_next_to_an_orange_bag.jpg/500px-Zimba_chips_next_to_an_orange_bag.jpg";
const waferBar1 = "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d0/Lion-Bar-Split.png/500px-Lion-Bar-Split.png";
const waferBar2 = "https://upload.wikimedia.org/wikipedia/commons/thumb/f/f1/Tunnocks-Caramel-Wafer-Split.jpg/500px-Tunnocks-Caramel-Wafer-Split.jpg";

// Real photos of the client's own products, supplied directly and hosted via Firebase Hosting
// (admin-web/public/product-images/ — static files are served before the SPA rewrite kicks in,
// so this needs no Cloud Storage / Blaze plan). Base URL is the live admin-web hosting origin.
const PRODUCT_IMAGE_BASE = "https://mackson-delivery.web.app/product-images";
const snowflakeFlour = `${PRODUCT_IMAGE_BASE}/snowflake-cake-flour-2-5kg.webp`;
const roycoShisanyama = `${PRODUCT_IMAGE_BASE}/royco-shisanyama-coat-cook.webp`;
const manhattanCrazy = `${PRODUCT_IMAGE_BASE}/manhattan-crazy-creatures.webp`;
const mamasChilliSauce = `${PRODUCT_IMAGE_BASE}/mamas-chilli-sauce-750ml.webp`;
const mamasChipDip = `${PRODUCT_IMAGE_BASE}/mamas-chip-dip-750ml.webp`;
const mamasMayo = `${PRODUCT_IMAGE_BASE}/mamas-mayonnaise.webp`;
const maqWashingPowder = `${PRODUCT_IMAGE_BASE}/maq-washing-powder-1kg.png`;
const nandosExtraHot = `${PRODUCT_IMAGE_BASE}/nandos-peri-peri-extra-hot-250g.jpg`;
const nandosHot = `${PRODUCT_IMAGE_BASE}/nandos-peri-peri-hot-250g.jpg`;
const nandosMedium = `${PRODUCT_IMAGE_BASE}/nandos-peri-peri-medium-250g.jpg`;
const jokoRooibos80s = `${PRODUCT_IMAGE_BASE}/joko-rooibos-80s.jpg`;
const newdenPutty = `${PRODUCT_IMAGE_BASE}/newden-putty-500g.webp`;
const omoWashingPowder = `${PRODUCT_IMAGE_BASE}/omo-auto-washing-powder-4kg.webp`;
const tangoSport = `${PRODUCT_IMAGE_BASE}/tango-sport-mango-500ml.png`;
const marieBiscuits = `${PRODUCT_IMAGE_BASE}/marie-oven-fresh-biscuits-150g.jpg`;
const royalEclairs = `${PRODUCT_IMAGE_BASE}/royal-eclairs-caramel-toffees.jpg`;
const selatiSugar = `${PRODUCT_IMAGE_BASE}/selati-white-sugar-2-5kg.webp`;
const willardsCheasNaks = `${PRODUCT_IMAGE_BASE}/willards-cheas-naks-spicy-tomato-135g.webp`;
const nolaSpread = `${PRODUCT_IMAGE_BASE}/nola-sandwich-spread-270g.jpg`;
const imanaSoup = `${PRODUCT_IMAGE_BASE}/imana-beef-onion-soup-45g.jpg`;

export const products = [
  // Bakery (aisle 1) — the two MACKSONS items are the client's own in-house bakery brand.
  { productId: "prod-bread-white", name: "Mackson's White Bread 600g", description: "Our own in-house bakery white bread, baked fresh daily.", category: "Bakery", aisleNumber: 1, unitPrice: 10.99, weightGrams: 600, currentStockLevel: 60, barcode: "87994", imageUrl: bread },
  { productId: "prod-bread-unsliced", name: "Mackson's White Bread Loaf Unsliced", description: "Our own in-house bakery unsliced white loaf.", category: "Bakery", aisleNumber: 1, unitPrice: 12.99, weightGrams: 700, currentStockLevel: 40, barcode: "6009628580776", imageUrl: breadLoafPlain },
  { productId: "prod-bread-brown-sunshine", name: "Sunshine Brown Bread 700g", description: "Wholesome sliced brown bread.", category: "Bakery", aisleNumber: 1, unitPrice: 14.99, weightGrams: 700, currentStockLevel: 45, barcode: "6009827160014", imageUrl: breadBrown1 },
  { productId: "prod-bread-brown-albany", name: "Albany Brown Bread 700g", description: "Sliced brown bread, a household favourite.", category: "Bakery", aisleNumber: 1, unitPrice: 18.99, weightGrams: 700, currentStockLevel: 45, barcode: "6001253010185", imageUrl: breadBrown2 },

  // Dairy (aisle 3)
  { productId: "prod-milk-azore", name: "Azore Full Cream Milk 1L", description: "Full cream fresh milk.", category: "Dairy", aisleNumber: 3, unitPrice: 13.99, weightGrams: 1000, currentStockLevel: 50, barcode: "6009706342487", imageUrl: milk },
  { productId: "prod-eggs-barneys", name: "Barneys Free Range Eggs Mixed 18s", description: "Free range eggs, mixed sizes, 18 pack.", category: "Dairy", aisleNumber: 3, unitPrice: 69.99, weightGrams: 1100, currentStockLevel: 20, barcode: "6009879737790", imageUrl: eggs },
  { productId: "prod-yoghurt-mayo", name: "Mayo Dairy Drinking Yoghurt Strawberry 120g", description: "Drinking yoghurt, strawberry flavour.", category: "Dairy", aisleNumber: 3, unitPrice: 3.99, weightGrams: 120, currentStockLevel: 80, barcode: "6002004001209", imageUrl: yoghurtStrawberry },

  // Fruit & Veg (aisle 5) — priced per kg on the client's scales; treated here as a ~1kg pack.
  { productId: "prod-bananas", name: "Bananas", description: "Fresh bananas, priced per kg.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 34.95, weightGrams: 1000, currentStockLevel: 70, barcode: "3224", imageUrl: bananas },
  { productId: "prod-apples-red", name: "Apples Red 1kg", description: "Crisp red apples.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 17.99, weightGrams: 1000, currentStockLevel: 55, barcode: "50319", imageUrl: apple },
  { productId: "prod-tomatoes", name: "Tomatoes Pre Pack 1kg", description: "Fresh, pre-packed tomatoes.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 39.99, weightGrams: 1000, currentStockLevel: 0, barcode: "50193", imageUrl: tomatoes },
  { productId: "prod-potatoes", name: "Potatoes 1kg", description: "Fresh potatoes.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 16.99, weightGrams: 1000, currentStockLevel: 65, barcode: "50979", imageUrl: potato },
  { productId: "prod-onions", name: "Onions 1kg", description: "Fresh onions.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 4.99, weightGrams: 1000, currentStockLevel: 90, barcode: "41582", imageUrl: onion },
  { productId: "prod-carrots-rugani", name: "Rugani Carrots 1kg", description: "Fresh carrots.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 19.99, weightGrams: 1000, currentStockLevel: 60, barcode: "50310", imageUrl: carrot },
  { productId: "prod-butternut", name: "Butternut", description: "Fresh butternut squash, priced per kg.", category: "Fruit & Veg", aisleNumber: 5, unitPrice: 12.99, weightGrams: 1000, currentStockLevel: 40, barcode: "50082", imageUrl: butternut },

  // Meat & Fish (aisle 7)
  { productId: "prod-chicken-breast", name: "Chicken Breast Fillet 1kg", description: "Fresh chicken breast fillets.", category: "Meat", aisleNumber: 7, unitPrice: 46.90, weightGrams: 1000, currentStockLevel: 35, barcode: "6009604130506", imageUrl: chicken },
  { productId: "prod-sole-fish", name: "Sole Fish", description: "Fresh sole fish, priced per kg.", category: "Meat", aisleNumber: 7, unitPrice: 11.40, weightGrams: 1000, currentStockLevel: 15, barcode: "5", imageUrl: fish },

  // Pantry (aisle 9)
  { productId: "prod-rice-spekko", name: "Spekko Saman White Rice 1kg", description: "Long grain white rice.", category: "Pantry", aisleNumber: 9, unitPrice: 43.99, weightGrams: 1000, currentStockLevel: 50, barcode: "6001571121730", imageUrl: riceSack1 },
  { productId: "prod-rice-super-thai", name: "Super Thai White Rice 500g", description: "Fragrant Thai white rice.", category: "Pantry", aisleNumber: 9, unitPrice: 14.99, weightGrams: 500, currentStockLevel: 55, barcode: "6009804730230", imageUrl: riceSack2 },
  { productId: "prod-spaghetti-oba", name: "Oba Classic Spaghetti 500g", description: "Durum wheat spaghetti.", category: "Pantry", aisleNumber: 9, unitPrice: 7.29, weightGrams: 500, currentStockLevel: 70, barcode: "8690828000034", imageUrl: pasta },
  { productId: "prod-spaghetti-fattis", name: "Fatti's & Moni's Spaghetti 500g", description: "A South African pantry staple.", category: "Pantry", aisleNumber: 9, unitPrice: 21.99, weightGrams: 500, currentStockLevel: 60, barcode: "6009522300098", imageUrl: spaghetti2 },
  { productId: "prod-beans-koo", name: "KOO Baked Beans 420g", description: "Baked beans in tomato sauce.", category: "Pantry", aisleNumber: 9, unitPrice: 10.99, weightGrams: 420, currentStockLevel: 65, barcode: "6001024021228", imageUrl: beans },
  { productId: "prod-peanut-butter-champion", name: "Champion Peanut Butter 1kg", description: "Smooth peanut butter.", category: "Pantry", aisleNumber: 9, unitPrice: 32.99, weightGrams: 1000, currentStockLevel: 40, barcode: "6009697952894", imageUrl: peanutButter },
  { productId: "prod-mayo-crosse", name: "Crosse & Blackwell Mayonnaise 250g", description: "Classic mayonnaise.", category: "Pantry", aisleNumber: 9, unitPrice: 23.99, weightGrams: 250, currentStockLevel: 45, barcode: "6001068027408", imageUrl: mayo },
  { productId: "prod-sugar-first", name: "First Sugar White 12.5kg", description: "White sugar, catering pack.", category: "Pantry", aisleNumber: 9, unitPrice: 82.95, weightGrams: 12500, currentStockLevel: 20, barcode: "6009692730220", imageUrl: sugar },
  { productId: "prod-oil-goldenglo", name: "Golden Glo Sunflower Oil 5L", description: "Pure sunflower cooking oil.", category: "Pantry", aisleNumber: 9, unitPrice: 149.99, weightGrams: 5000, currentStockLevel: 18, barcode: "6004493000305", imageUrl: oil },
  { productId: "prod-flour-snowflake", name: "Snowflake Cake Wheat Flour 2.5kg", description: "Cake flour for baking.", category: "Pantry", aisleNumber: 9, unitPrice: 45.99, weightGrams: 2500, currentStockLevel: 40, barcode: "6001205102562", imageUrl: snowflakeFlour },
  { productId: "prod-coating-royco-shisanyama", name: "Royco Shisanyama Coat & Cook 38g", description: "Coat & cook seasoning for beef, chicken or lamb, ideal for braai. Not in the client's price list — priced by comparison to similar Royco sachets.", category: "Pantry", aisleNumber: 9, unitPrice: 7.99, weightGrams: 38, currentStockLevel: 55, barcode: "0000000000004", imageUrl: roycoShisanyama },
  { productId: "prod-sauce-mamas-chilli", name: "Mama's Chilli Sauce 750ml", description: "Chilli sauce. Not in the client's price list — priced the same as Mama's Chip-Dip Sauce 750ml, the closest same-brand, same-size match.", category: "Pantry", aisleNumber: 9, unitPrice: 41.99, weightGrams: 750, currentStockLevel: 40, barcode: "0000000000005", imageUrl: mamasChilliSauce },
  { productId: "prod-sauce-mamas-chipdip", name: "Mama's Peri Chip-Dip Sauce 750ml", description: "Peri chip-dip sauce.", category: "Pantry", aisleNumber: 9, unitPrice: 41.99, weightGrams: 750, currentStockLevel: 40, barcode: "6004101011372", imageUrl: mamasChipDip },
  { productId: "prod-mayo-mamas", name: "Mama's Original Mayonnaise", description: "Original mayonnaise. Not in the client's price list — priced by comparison to similarly sized mayonnaise jars.", category: "Pantry", aisleNumber: 9, unitPrice: 44.99, weightGrams: 700, currentStockLevel: 35, barcode: "0000000000006", imageUrl: mamasMayo },
  { productId: "prod-sauce-nandos-xhot", name: "Nando's Peri-Peri Sauce Extra Hot 250ml", description: "Extra hot peri-peri sauce, extra fire, extra flavour.", category: "Pantry", aisleNumber: 9, unitPrice: 53.99, weightGrams: 250, currentStockLevel: 45, barcode: "6003770000670", imageUrl: nandosExtraHot },
  { productId: "prod-sauce-nandos-hot", name: "Nando's Peri-Peri Sauce Hot 250ml", description: "Hot peri-peri sauce, full-on fiery flavour.", category: "Pantry", aisleNumber: 9, unitPrice: 53.99, weightGrams: 250, currentStockLevel: 45, barcode: "6003770000663", imageUrl: nandosHot },
  { productId: "prod-sauce-nandos-medium", name: "Nando's Peri-Peri Sauce Medium 250ml", description: "Medium peri-peri sauce, full-on flavour, half-way heat.", category: "Pantry", aisleNumber: 9, unitPrice: 53.99, weightGrams: 250, currentStockLevel: 45, barcode: "6003770000168", imageUrl: nandosMedium },
  { productId: "prod-sugar-selati", name: "Selati White Sugar 2.5kg", description: "Pure white sugar.", category: "Pantry", aisleNumber: 9, unitPrice: 56.99, weightGrams: 2500, currentStockLevel: 35, barcode: "6001345000209", imageUrl: selatiSugar },
  { productId: "prod-spread-nola", name: "Nola Traditional Sandwich Spread 270g", description: "Traditional sandwich spread.", category: "Pantry", aisleNumber: 9, unitPrice: 29.99, weightGrams: 270, currentStockLevel: 40, barcode: "6001069019990", imageUrl: nolaSpread },
  { productId: "prod-soup-imana-beefonion", name: "Imana Beef & Onion Flavoured Soup 45g", description: "Packet soup, beef and onion flavour.", category: "Pantry", aisleNumber: 9, unitPrice: 4.99, weightGrams: 45, currentStockLevel: 80, barcode: "6002657777858", imageUrl: imanaSoup },

  // Beverages (aisle 11)
  { productId: "prod-cola-vision", name: "Vision Cooldrinks Cola 2L", description: "Cola-flavoured cooldrink.", category: "Beverages", aisleNumber: 11, unitPrice: 11.99, weightGrams: 2000, currentStockLevel: 55, barcode: "6004101063166", imageUrl: cola },
  { productId: "prod-ironbrew-roxy", name: "Roxy Cooldrink Iron Brew 2L", description: "Iron Brew flavoured cooldrink.", category: "Beverages", aisleNumber: 11, unitPrice: 4.99, weightGrams: 2000, currentStockLevel: 50, barcode: "6009880860449", imageUrl: irnBru },
  { productId: "prod-tea-fiveroses", name: "Five Roses Tagged Tea Bags 25s", description: "Classic South African tea.", category: "Beverages", aisleNumber: 11, unitPrice: 38.99, weightGrams: 62, currentStockLevel: 60, barcode: "6001156170054", imageUrl: teaBags },
  { productId: "prod-tea-joko", name: "Joko Rooibos Tea Bags 80s", description: "Naturally caffeine-free rooibos tea, locally sourced.", category: "Beverages", aisleNumber: 11, unitPrice: 49.99, weightGrams: 200, currentStockLevel: 60, barcode: "6001087378970", imageUrl: jokoRooibos80s },
  { productId: "prod-tango-sport-mango", name: "Tango Sport Mango 500ml", description: "Mango-flavoured dairy fruit mix sports drink. Not in the client's price list — priced by comparison to similarly sized cooldrinks.", category: "Beverages", aisleNumber: 11, unitPrice: 15.99, weightGrams: 500, currentStockLevel: 40, barcode: "0000000000001", imageUrl: tangoSport },

  // Snacks (aisle 12)
  { productId: "prod-chips-simba-chicken", name: "Simba Chips Munchies Chicken 54g", description: "Chicken-flavoured potato chips.", category: "Snacks", aisleNumber: 12, unitPrice: 4.50, weightGrams: 54, currentStockLevel: 90, barcode: "6009710721957", imageUrl: chips },
  { productId: "prod-chips-simba-chillicheese", name: "Simba Chips Munchies Chilli Cheese 54g", description: "Chilli cheese-flavoured potato chips.", category: "Snacks", aisleNumber: 12, unitPrice: 4.50, weightGrams: 54, currentStockLevel: 90, barcode: "6009710721865", imageUrl: chipsBag2 },
  { productId: "prod-kitkat-chunky-mint", name: "Nestle Kit Kat Chunky Mint 45g", description: "Chocolate wafer bar, mint flavour.", category: "Snacks", aisleNumber: 12, unitPrice: 6.99, weightGrams: 45, currentStockLevel: 70, barcode: "6001068696406", imageUrl: waferBar1 },
  { productId: "prod-barone-coffee", name: "Nestle Bar One Coffee 21g", description: "Chocolate, caramel and coffee bar.", category: "Snacks", aisleNumber: 12, unitPrice: 7.99, weightGrams: 21, currentStockLevel: 70, barcode: "6009188008758", imageUrl: waferBar2 },
  { productId: "prod-biscuits-marie-ovenfresh", name: "Oven Fresh Marie Biscuits 150g", description: "The original Marie biscuits.", category: "Snacks", aisleNumber: 12, unitPrice: 7.99, weightGrams: 150, currentStockLevel: 70, barcode: "6009611950012", imageUrl: marieBiscuits },
  { productId: "prod-sweets-manhattan-crazy", name: "Manhattan Crazy Creatures 125g", description: "Sugar coated fruit and cola flavoured jellies. Not in the client's price list — priced by comparison to similarly sized Manhattan sweets.", category: "Snacks", aisleNumber: 12, unitPrice: 27.99, weightGrams: 125, currentStockLevel: 50, barcode: "0000000000002", imageUrl: manhattanCrazy },
  { productId: "prod-sweets-royal-eclairs", name: "Good Luck Royal Eclairs Caramel Toffees 100s", description: "Caramel flavoured toffees, 100 pieces. Not in the client's price list — priced by comparison to similar toffee/sweet bags.", category: "Snacks", aisleNumber: 12, unitPrice: 39.99, weightGrams: 400, currentStockLevel: 40, barcode: "0000000000003", imageUrl: royalEclairs },
  { productId: "prod-cheasnaks-willards", name: "Willards Cheas Naks Spicy Tomato 135g", description: "Spicy tomato flavoured maize snack.", category: "Snacks", aisleNumber: 12, unitPrice: 18.99, weightGrams: 135, currentStockLevel: 60, barcode: "6009702441429", imageUrl: willardsCheasNaks },

  // Household (aisle 13) — added for washing powders and household items, which didn't fit the
  // existing taxonomy from Part 1.
  { productId: "prod-washingpowder-maq", name: "MAQ Hand Washing Powder 1kg", description: "Super stain busting hand washing powder.", category: "Household", aisleNumber: 13, unitPrice: 44.99, weightGrams: 1000, currentStockLevel: 35, barcode: "6009678261465", imageUrl: maqWashingPowder },
  { productId: "prod-washingpowder-omo", name: "OMO Auto Washing Powder 4kg", description: "Unbeatable stain removal auto washing powder, 40 washes.", category: "Household", aisleNumber: 13, unitPrice: 199.99, weightGrams: 4000, currentStockLevel: 25, barcode: "6001087383820", imageUrl: omoWashingPowder },
  { productId: "prod-putty-newden", name: "Newden General Purpose Putty 500g", description: "General purpose wall and surface filler putty.", category: "Household", aisleNumber: 13, unitPrice: 7.99, weightGrams: 500, currentStockLevel: 30, barcode: "6002024080017", imageUrl: newdenPutty }
];

async function ensureAuthUser(auth: Auth, email: string, role: string, uidHint: string): Promise<string> {
  try {
    const existing = await auth.getUserByEmail(email);
    await auth.setCustomUserClaims(existing.uid, { role });
    return existing.uid;
  } catch {
    const created = await auth.createUser({ uid: uidHint, email, password: "Password123!", displayName: role });
    await auth.setCustomUserClaims(created.uid, { role });
    return created.uid;
  }
}

async function deleteLegacyStore(db: Firestore): Promise<void> {
  const legacyRef = db.collection("storeNodes").doc(LEGACY_STORE_ID);
  const legacySnap = await legacyRef.get();
  if (!legacySnap.exists) return;

  const productsSnap = await legacyRef.collection("products").get();
  await Promise.all(productsSnap.docs.map((doc) => doc.ref.delete()));
  await legacyRef.delete();
  console.log(`Removed legacy store document "${LEGACY_STORE_ID}" (${productsSnap.size} products).`);
}

export async function seedCatalogueAndStaff(db: Firestore, auth: Auth): Promise<void> {
  await deleteLegacyStore(db);

  await db.collection("storeNodes").doc(STORE_ID).set({
    storeId: STORE_ID,
    // "12 Albert Street" and the branch name/photo below come straight from the Part 1
    // prototype's own "Choose nearest store" screen (a real photo of the actual storefront) —
    // more authoritative than the "180 Albert Street" address an earlier web search turned up,
    // which was for a similarly-named but different listing.
    branchName: "Macksons Main Store",
    address: "12 Albert Street, Estcourt, KwaZulu-Natal",
    imageUrl: `${PRODUCT_IMAGE_BASE}/store-estcourt-main.png`,
    latitude: -29.0050,
    longitude: 29.8680,
    serviceRadiusKm: 8,
    isAcceptingOrders: true
  });

  await Promise.all(
    products.map((product) =>
      db
        .collection("storeNodes")
        .doc(STORE_ID)
        .collection("products")
        .doc(product.productId)
        .set({
          ...product,
          storeId: STORE_ID,
          isAgeRestricted: false,
          nameSearchKey: product.name.toLowerCase()
        })
    )
  );

  const pickerUid = await ensureAuthUser(auth, "picker@mackson.demo", "PICKER", "demo-picker-uid");
  await db.collection("pickers").doc(pickerUid).set({
    shopperId: pickerUid,
    staffName: "Demo Picker",
    assignedStoreId: STORE_ID,
    dutyStatus: "ON_DUTY"
  });

  const driverUid = await ensureAuthUser(auth, "driver@mackson.demo", "DRIVER", "demo-driver-uid");
  await db.collection("drivers").doc(driverUid).set({
    driverId: driverUid,
    driverName: "Demo Driver",
    vehicleType: "Motorbike",
    // Shown on the customer's tracking screen for door-side verification (WIL group
    // requirement) — a placeholder KZN-format plate and a generic, freely-licensed photo since
    // this is demo/seed data, not a real courier.
    licensePlate: "ND 12 FG KZN",
    photoUrl: "https://upload.wikimedia.org/wikipedia/commons/thumb/7/7c/Profile_avatar_placeholder_large.png/500px-Profile_avatar_placeholder_large.png",
    currentPayoutBalance: 0,
    isAvailable: true
  });

  const managerUid = await ensureAuthUser(auth, "manager@mackson.demo", "MANAGER", "demo-manager-uid");
  await db.collection("users").doc(managerUid).set({
    customerId: managerUid,
    name: "Demo Store Manager",
    email: "manager@mackson.demo",
    role: "MANAGER"
  });

  console.log("Seed complete:");
  console.log(`  Store:   ${STORE_ID} (${products.length} products)`);
  console.log("  Picker:  picker@mackson.demo / Password123!");
  console.log("  Driver:  driver@mackson.demo / Password123!");
  console.log("  Manager: manager@mackson.demo / Password123!");
  console.log("  (Register a normal customer account from the app's Register screen.)");
}
