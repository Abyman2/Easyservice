<script>
  import { onMount } from 'svelte';
  import Navbar from './lib/components/Navbar.svelte';
  import ListingCard from './lib/components/ListingCard.svelte';
  import BookingModal from './lib/components/BookingModal.svelte';
  import BookingDetailsModal from './lib/components/BookingDetailsModal.svelte';
  import LoginModal from './lib/components/LoginModal.svelte';
  import ProviderShowcaseModal from './lib/components/ProviderShowcaseModal.svelte';
  import SpinWheelModal from './lib/components/SpinWheelModal.svelte';
  import EasyToolsModal from './lib/components/EasyToolsModal.svelte';
  import RegisterModal from './lib/components/RegisterModal.svelte';
  import Icon from './lib/components/Icon.svelte';
  import { fetchListings, mockProviders } from './lib/api/api.js';
  import { currentUser } from './lib/stores/authStore.js';
  import { userBookings, cancelBookingItem } from './lib/stores/bookingStore.js';

  let activeTab = 'listings'; // 'listings' | 'history' | 'provider'
  let profileSubTab = 'BOOKINGS'; // 'BOOKINGS' | 'FAVORITES'
  let selectedCategory = 'ALL';
  let selectedLocation = 'ALL';
  let searchQuery = '';
  let sortBy = 'RECOMMENDED';
  let currentTheme = 'light';
  let onlyVerified = false;

  let listings = [];
  let favoriteIds = new Set(['h_prov_1', 'c_prov_1', 's_prov_1']); // Default pre-liked favorites
  let selectedProviderListing = null;
  let selectedListing = null;
  let selectedBookingPass = null;
  let showSpinModal = false;
  let showToolsModal = false;
  let showRegisterModal = false;

  // Thematic transition animation trigger
  let transitionCategory = null;
  let isTransitioning = false;

  // Category specific search fields
  let checkInDate = '';
  let checkOutDate = '';
  let guestCount = 2;
  let vehicleType = '4X4';

  // Provider Onboarding & Dashboard State
  let isCompanyRegistered = false;
  let companyName = '';
  let companyCategory = 'HOTEL';
  let companyLocation = 'Addis Ababa';
  let companyPhone = '+251 91 123 4567';

  // Provider Add Listing Modal State
  let showAddListingModal = false;
  let newTitle = '';
  let newType = 'Executive Suite';
  let newPrice = 2500;
  let newCapacity = 10;
  let newImage = 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80';
  let newDesc = '';
  let providerPublishedListings = [];

  // Context-aware Hero Atmosphere Configurations
  const categoryAtmospheres = {
    ALL: {
      tag: 'ETHIOPIA MARKETPLACE',
      headline: 'One place for everything worth experiencing in Ethiopia.',
      subtitle: 'Discover verified luxury resort stays, 4×4 rentals, cultural jazz summits, and authentic Ethiopian crafts.',
      bgImg: 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=1600&q=80',
      accentColor: 'var(--accent-gold)'
    },
    HOTEL: {
      tag: 'STAYS • 7 VERIFIED HOTELS & RESORTS',
      headline: 'Stay somewhere unforgettable in Ethiopia.',
      subtitle: 'Browse 7 top hotel providers: Kuriftu, Skylight, Haile Resort, Sheraton, Radisson Blu, Lalibela Lodge & Grand Hotel.',
      bgImg: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1600&q=80',
      accentColor: '#C89B3C'
    },
    CAR_RENTAL: {
      tag: 'DRIVE • 7 VERIFIED CAR FLEETS',
      headline: 'Go further across Ethiopia.',
      subtitle: 'Rent 4×4 Land Cruisers, VIP sedans, and safari vehicles from 7 trusted Ethiopian car rental providers.',
      bgImg: 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=1600&q=80',
      accentColor: '#3B82F6'
    },
    EVENT: {
      tag: 'EXPERIENCES • 7 VERIFIED EVENT ORGANIZERS',
      headline: "What's happening in Ethiopia?",
      subtitle: 'Book passes for African Jazz Summit, Great Ethiopian Run, Timkat festival & cultural fairs.',
      bgImg: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1600&q=80',
      accentColor: '#E11D48'
    },
    STORE: {
      tag: 'SHOP • 7 ETHIOPIAN ARTISAN SHOPS',
      headline: 'Bring Ethiopia home.',
      subtitle: 'Shop Yirgacheffe coffee, handwoven habesha kemis dresses, silver jewelry & genuine leather goods.',
      bgImg: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=1600&q=80',
      accentColor: '#10B981'
    }
  };

  const ethiopianLocations = [
    { id: 'ALL', name: 'All Ethiopia', icon: 'globe' },
    { id: 'Addis', name: 'Addis Ababa', icon: 'city' },
    { id: 'Bishoftu', name: 'Bishoftu Lakefront', icon: 'wave' },
    { id: 'Hawassa', name: 'Lake Hawassa', icon: 'wave' },
    { id: 'Lalibela', name: 'Lalibela Highlands', icon: 'mountain' },
    { id: 'Bahir Dar', name: 'Bahir Dar & Tana', icon: 'boat' }
  ];

  onMount(async () => {
    const savedTheme = localStorage.getItem('easyservice_theme') || 'light';
    currentTheme = savedTheme;
    document.documentElement.setAttribute('data-theme', savedTheme);

    const savedFavs = localStorage.getItem('easyservice_favorites');
    if (savedFavs) {
      try {
        favoriteIds = new Set(JSON.parse(savedFavs));
      } catch {
        /* keep defaults */
      }
    }

    const data = await fetchListings();
    listings = data && data.length > 0 ? data : mockProviders;
  });

  function toggleTheme() {
    currentTheme = currentTheme === 'light' ? 'dark' : 'light';
    localStorage.setItem('easyservice_theme', currentTheme);
    document.documentElement.setAttribute('data-theme', currentTheme);
  }

  function handleSelectCategory(cat) {
    if (selectedCategory === cat) return;
    transitionCategory = cat;
    isTransitioning = true;
    selectedCategory = cat;

    setTimeout(() => {
      isTransitioning = false;
    }, 700);
  }

  function toggleFavorite(id) {
    if (favoriteIds.has(id)) {
      favoriteIds.delete(id);
    } else {
      favoriteIds.add(id);
    }
    favoriteIds = new Set(favoriteIds);
    localStorage.setItem('easyservice_favorites', JSON.stringify([...favoriteIds]));
  }

  $: currentAtmosphere = categoryAtmospheres[selectedCategory] || categoryAtmospheres.ALL;

  $: filteredListings = listings.filter(l => {
    const matchCat = selectedCategory === 'ALL' || l.category === selectedCategory;
    const matchLoc = selectedLocation === 'ALL' || (l.location && l.location.toLowerCase().includes(selectedLocation.toLowerCase()));
    const q = (searchQuery || '').trim().toLowerCase();
    const matchSearch = !q || [l.title, l.description, l.hostName, l.location].some((field) => (field || '').toLowerCase().includes(q));
    const matchVerified = !onlyVerified || l.status === 'PUBLISHED';
    const matchGuests = selectedCategory !== 'HOTEL' || Number(guestCount) <= (l.capacity || 1);
    const haystack = `${l.title} ${l.description} ${(l.variants || []).map((v) => v.title).join(' ')}`.toLowerCase();
    const matchVehicle = selectedCategory !== 'CAR_RENTAL' || vehicleType === 'ALL' ||
      (vehicleType === '4X4' && /4x4|4×4|land cruiser|offroad|off-road/.test(haystack)) ||
      (vehicleType === 'SUV' && /suv|tucson|crossover/.test(haystack)) ||
      (vehicleType === 'SEDAN' && /sedan|e-class|executive|vip/.test(haystack));
    return matchCat && matchLoc && matchSearch && matchVerified && matchGuests && matchVehicle;
  }).sort((a, b) => {
    if (sortBy === 'PRICE_LOW') return a.price - b.price;
    if (sortBy === 'PRICE_HIGH') return b.price - a.price;
    return 0;
  });

  $: favoritedListings = listings.filter(l => favoriteIds.has(l.id));

  $: trendingListings = listings.slice(0, 3);

  // Customer Profile Spending Analytics
  $: hotelBookings = ($userBookings || []).filter(b => b.category === 'HOTEL' && b.status === 'CONFIRMED');
  $: carBookings = ($userBookings || []).filter(b => b.category === 'CAR_RENTAL' && b.status === 'CONFIRMED');
  $: eventBookings = ($userBookings || []).filter(b => b.category === 'EVENT' && b.status === 'CONFIRMED');
  $: storeBookings = ($userBookings || []).filter(b => b.category === 'STORE' && b.status === 'CONFIRMED');

  $: hotelSpent = hotelBookings.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
  $: carSpent = carBookings.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
  $: eventSpent = eventBookings.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
  $: storeSpent = storeBookings.reduce((sum, b) => sum + (b.totalAmount || 0), 0);
  $: totalSpentLifetime = hotelSpent + carSpent + eventSpent + storeSpent;

  function handleRegisterCompany() {
    if (!companyName) return;
    isCompanyRegistered = true;
    providerSuccessMsg = `Company "${companyName}" registered successfully! Welcome to your Provider Dashboard.`;
  }

  function handleCreateProviderListing() {
    if (!newTitle || newPrice <= 0 || newCapacity <= 0) return;

    const newListing = {
      id: 'prov_' + Date.now(),
      title: `${companyName} — ${newTitle}`,
      category: companyCategory,
      description: newDesc || `Offered directly by ${companyName}.`,
      price: Number(newPrice),
      capacity: Number(newCapacity),
      availableQuantity: Number(newCapacity),
      status: 'PUBLISHED',
      location: companyLocation || 'Addis Ababa',
      hostName: companyName,
      imageUrl: newImage,
      variants: [
        { id: 'v_custom_1', title: `${newTitle} (${newType})`, desc: newDesc || 'High quality service option.', price: Number(newPrice), availableCount: Number(newCapacity), unitLabel: companyCategory === 'HOTEL' ? '/ night' : (companyCategory === 'CAR_RENTAL' ? '/ day' : '/ item'), badge: newType }
      ]
    };

    listings = [newListing, ...listings];
    providerPublishedListings = [newListing, ...providerPublishedListings];
    providerSuccessMsg = `New listing "${newTitle}" created and published successfully under ${companyName}!`;
    showAddListingModal = false;
    newTitle = '';
    newDesc = '';
  }

  function handleSwitchUser(u) {
    $currentUser = u;
    userBookings.set([]); // Start as new for the new user
  }

  function handleCancelBooking(bookingId) {
    const refund = cancelBookingItem(bookingId);
    if (refund > 0) {
      currentUser.update((u) => u ? { ...u, balance: Number(u.balance || 0) + refund } : u);
    }
  }

  function openListing(listing) {
    selectedProviderListing = listing;
    activeTab = 'listings';
  }

  function scrollToResults() {
    const el = document.querySelector('.marketplace-browsing-section');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  function goToCategory(cat) {
    activeTab = 'listings';
    selectedProviderListing = null;
    handleSelectCategory(cat);
    setTimeout(scrollToResults, 50);
  }

  function goToLocation(locId) {
    activeTab = 'listings';
    selectedProviderListing = null;
    selectedLocation = locId;
    setTimeout(scrollToResults, 50);
  }
</script>

<main class="app-container">
  <!-- Category Switcher Transition Banner Animation -->
  {#if isTransitioning}
    <div class="category-transition-overlay animate-fade-in">
      <div class="transition-icon-mover">
        {#if transitionCategory === 'CAR_RENTAL'}
          <div class="moving-vehicle">
            <Icon name="car" size={38} color="var(--accent-gold)" />
            <span>Cruising to Drive 4x4s & Car Fleets...</span>
          </div>
        {:else if transitionCategory === 'HOTEL'}
          <div class="moving-vehicle">
            <Icon name="bed" size={38} color="var(--accent-gold)" />
            <span>Opening Hotels & Luxury Resorts...</span>
          </div>
        {:else if transitionCategory === 'EVENT'}
          <div class="moving-vehicle">
            <Icon name="ticket" size={38} color="var(--accent-gold)" />
            <span>Unlocking Ethiopian Experiences...</span>
          </div>
        {:else}
          <div class="moving-vehicle">
            <Icon name="bag" size={38} color="var(--accent-gold)" />
            <span>Discovering Artisan Coffee & Kemis Crafts...</span>
          </div>
        {/if}
      </div>
    </div>
  {/if}

  <!-- Navbar Header -->
  <Navbar 
    bind:activeTab 
    bind:selectedCategory 
    {currentTheme} 
    {toggleTheme} 
    openSpinModal={() => showSpinModal = true} 
    openRegisterModal={() => showRegisterModal = true} 
    onSwitchUser={handleSwitchUser} 
  />

  <div class="main-content">
    {#if activeTab === 'listings'}
      {#if selectedProviderListing}
        <!-- Standalone Full Property Detail Page -->
        <ProviderShowcaseModal 
          listing={selectedProviderListing} 
          onClose={() => selectedProviderListing = null} 
          onSelectVariant={(variant) => { selectedProviderListing = null; selectedListing = variant; }} 
        />
      {:else}
        <!-- Hero Atmosphere & Search Module -->
      <section class="hero-section animate-fade-in" style="--accent-current: {currentAtmosphere.accentColor};">
        <div class="hero-bg-wrapper">
          <img 
            src={currentAtmosphere.bgImg} 
            alt={currentAtmosphere.headline} 
            class="hero-bg-img" 
          />
          <div class="hero-overlay"></div>
        </div>

        <div class="hero-content-box">
          <span class="hero-pill-badge">{currentAtmosphere.tag}</span>
          <h1 class="hero-headline">{currentAtmosphere.headline}</h1>
          <p class="hero-subtext">{currentAtmosphere.subtitle}</p>

          <!-- Category Experience Switcher Tabs -->
          <div class="category-experience-tabs">
            <button class="experience-tab {selectedCategory === 'ALL' ? 'active' : ''}" on:click={() => handleSelectCategory('ALL')}>
              <Icon name="sparkles" size={14} /> Explore All (28 Businesses)
            </button>
            <button class="experience-tab {selectedCategory === 'HOTEL' ? 'active' : ''}" on:click={() => handleSelectCategory('HOTEL')}>
              <Icon name="bed" size={14} /> 7 Hotels & Stays
            </button>
            <button class="experience-tab {selectedCategory === 'CAR_RENTAL' ? 'active' : ''}" on:click={() => handleSelectCategory('CAR_RENTAL')}>
              <Icon name="car" size={14} /> 7 Car Fleets
            </button>
            <button class="experience-tab {selectedCategory === 'EVENT' ? 'active' : ''}" on:click={() => handleSelectCategory('EVENT')}>
              <Icon name="ticket" size={14} /> 7 Events
            </button>
            <button class="experience-tab {selectedCategory === 'STORE' ? 'active' : ''}" on:click={() => handleSelectCategory('STORE')}>
              <Icon name="bag" size={14} /> 7 Crafts Shops
            </button>
          </div>

          <!-- Context-Aware Search Module -->
          <div class="search-module marketplace-card">
            <div class="search-field">
              <label for="searchWhereInput">Where in Ethiopia?</label>
              <div class="input-with-icon">
                <Icon name="mappin" size={16} color="var(--accent-gold)" />
                <select id="searchWhereInput" bind:value={selectedLocation} class="search-select">
                  <option value="ALL">All Ethiopia Destinations</option>
                  <option value="Addis">Addis Ababa</option>
                  <option value="Bishoftu">Bishoftu Lakefront</option>
                  <option value="Hawassa">Lake Hawassa</option>
                  <option value="Lalibela">Lalibela Highlands</option>
                  <option value="Bahir Dar">Bahir Dar & Tana</option>
                </select>
              </div>
            </div>

            <div class="search-divider"></div>

            {#if selectedCategory === 'HOTEL'}
              <div class="search-field">
                <label for="checkInInput">Dates</label>
                <input id="checkInInput" type="date" bind:value={checkInDate} class="search-text-input" />
              </div>
              <div class="search-divider"></div>
              <div class="search-field">
                <label for="guestsSelect">Guests</label>
                <select id="guestsSelect" bind:value={guestCount} class="search-select">
                  <option value={1}>1 Guest</option>
                  <option value={2}>2 Guests (Suite)</option>
                  <option value={4}>4 Guests (Family)</option>
                </select>
              </div>
            {:else if selectedCategory === 'CAR_RENTAL'}
              <div class="search-field">
                <label for="vehicleTypeSelect">Vehicle Type</label>
                <select id="vehicleTypeSelect" bind:value={vehicleType} class="search-select">
                  <option value="4X4">4x4 Land Cruiser</option>
                  <option value="SUV">Luxury SUV</option>
                  <option value="SEDAN">Executive Sedan</option>
                </select>
              </div>
            {:else}
              <div class="search-field">
                <label for="searchQueryInput">Keyword Search</label>
                <input id="searchQueryInput" type="text" placeholder="e.g. Kuriftu, Skylight, Land Cruiser, Coffee..." bind:value={searchQuery} class="search-text-input" />
              </div>
            {/if}

            <button class="btn-gold search-btn">
              <Icon name="search" size={18} /> Search Businesses
            </button>
          </div>

          <!-- Location Quick Chips -->
          <div class="location-chips-row">
            <span class="chips-label">Popular Spots:</span>
            {#each ethiopianLocations as loc}
              <button 
                class="location-chip {selectedLocation === loc.id ? 'active' : ''}" 
                on:click={() => selectedLocation = loc.id}>
                <Icon name={loc.icon} size={12} /> {loc.name}
              </button>
            {/each}
          </div>
        </div>
      </section>



      <!-- Main Marketplace Grid Section -->
      <section class="marketplace-browsing-section">
        <!-- Sidebar Filters -->
        <aside class="filters-sidebar marketplace-card">
          <div class="filter-header">
            <h3>Filter Marketplace</h3>
            {#if selectedCategory !== 'ALL' || selectedLocation !== 'ALL' || searchQuery}
              <button class="clear-btn" on:click={() => { selectedCategory = 'ALL'; selectedLocation = 'ALL'; searchQuery = ''; }}>Reset</button>
            {/if}
          </div>

          <div class="filter-group">
            <label for="filterLocationSelect" class="filter-label">Destination</label>
            <select id="filterLocationSelect" bind:value={selectedLocation} class="input-field">
              <option value="ALL">All Destinations</option>
              <option value="Addis">Addis Ababa</option>
              <option value="Bishoftu">Bishoftu Lakefront</option>
              <option value="Hawassa">Lake Hawassa</option>
              <option value="Lalibela">Lalibela Highlands</option>
              <option value="Bahir Dar">Bahir Dar</option>
            </select>
          </div>

          <div class="filter-group">
            <label for="filterCategorySelect" class="filter-label">Category</label>
            <select id="filterCategorySelect" bind:value={selectedCategory} class="input-field">
              <option value="ALL">All 28 Businesses</option>
              <option value="HOTEL">7 Hotels & Resorts</option>
              <option value="CAR_RENTAL">7 Car Rentals</option>
              <option value="EVENT">7 Event Pass Organizers</option>
              <option value="STORE">7 Artisan Shops</option>
            </select>
          </div>
        </aside>

        <!-- Listings Grid Container -->
        <div class="listings-main-area">
          <div class="sort-header-row">
            <div>
              <h2 class="browse-title">
                {selectedCategory === 'ALL' ? 'All Verified Businesses (28 Total)' : categoryAtmospheres[selectedCategory].tag}
              </h2>
              <span class="results-count">{filteredListings.length} verified business providers listed</span>
            </div>

            <div class="sort-controls">
              <label for="sortBySelect" class="sort-label">Sort by:</label>
              <select id="sortBySelect" bind:value={sortBy} class="input-field sort-select">
                <option value="RECOMMENDED">Recommended</option>
                <option value="PRICE_LOW">Price: Low to High</option>
                <option value="PRICE_HIGH">Price: High to Low</option>
              </select>
            </div>
          </div>

          {#if filteredListings.length === 0}
            <div class="marketplace-card empty-state-box">
              <Icon name="search" size={36} color="var(--text-muted)" />
              <h3>No matching business providers found</h3>
              <p>Try adjusting your search location, category, or keyword filters.</p>
              <button class="btn-outline" on:click={() => { selectedCategory = 'ALL'; selectedLocation = 'ALL'; searchQuery = ''; }}>Reset All Filters</button>
            </div>
          {:else}
            <div class="listings-grid">
              {#each filteredListings as listing}
                <ListingCard 
                  {listing} 
                  isFavorite={favoriteIds.has(listing.id)} 
                  onToggleFavorite={toggleFavorite} 
                  onSelect={(l) => selectedProviderListing = l} 
                />
              {/each}
            </div>
          {/if}
        </div>
      </section>
      {/if}

    {:else if activeTab === 'deals'}
      <!-- 🔥 Section 15: Dedicated Hot Deals Marketplace Section -->
      <section class="hot-deals-section animate-fade-in">
        <div class="deals-hero-banner">
          <div>
            <span class="sub-pill">EXCLUSIVE LIMITED TIME OFFERS</span>
            <h2>🔥 Hot Deals & Promotional Discounts</h2>
            <p>Save up to 30% on luxury resorts, overland 4x4 safaris, festival passes, and handcrafted goods.</p>
          </div>
        </div>

        <div class="deals-grid">
          <!-- Deal 1 -->
          <div class="marketplace-card deal-card">
            <div class="deal-badge-overlay">20% OFF</div>
            <img src="https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80" alt="Kuriftu Resort Deal" class="deal-img" />
            <div class="deal-body">
              <span class="deal-category">🏨 HOTEL RESORT DEAL</span>
              <h3>🔥 Kuriftu Weekend Lakeside Deal</h3>
              <p class="deal-host">Kuriftu Resort & Spa Bishoftu</p>
              
              <div class="timer-box">
                <span>Ends in:</span>
                <strong class="timer-val">02d 14h 32m</strong>
              </div>

              <div class="deal-price-row">
                <div class="price-strikethrough-box">
                  <span class="orig-price">ETB 5,000</span>
                  <span class="discounted-price">ETB 4,000 / night</span>
                </div>
                <button class="btn-gold" on:click={() => { selectedListing = { ...mockProviders[0], title: '🔥 Kuriftu Weekend Lakeside Deal', price: 5000, preAppliedPromo: 'SUMMER20' }; }}>
                  Book Deal →
                </button>
              </div>
            </div>
          </div>

          <!-- Deal 2 -->
          <div class="marketplace-card deal-card">
            <div class="deal-badge-overlay">15% OFF</div>
            <img src="https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=800&q=80" alt="Car Rental Safari Deal" class="deal-img" />
            <div class="deal-body">
              <span class="deal-category">🚗 CAR RENTAL DEAL</span>
              <h3>🔥 5-Day Highland Safari Special</h3>
              <p class="deal-host">Kebede 4×4 Offroad Rentals</p>
              
              <div class="timer-box">
                <span>Ends in:</span>
                <strong class="timer-val">01d 08h 15m</strong>
              </div>

              <div class="deal-price-row">
                <div class="price-strikethrough-box">
                  <span class="orig-price">ETB 3,500</span>
                  <span class="discounted-price">ETB 2,975 / day</span>
                </div>
                <button class="btn-gold" on:click={() => { selectedListing = { ...mockProviders[7], title: '🔥 5-Day Highland Safari Special', price: 3500, preAppliedPromo: 'GOLD15' }; }}>
                  Book Deal →
                </button>
              </div>
            </div>
          </div>

          <!-- Deal 3 -->
          <div class="marketplace-card deal-card">
            <div class="deal-badge-overlay">30% OFF</div>
            <img src="https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80" alt="African Jazz Fest Deal" class="deal-img" />
            <div class="deal-body">
              <span class="deal-category">🎟 EVENT FESTIVAL DEAL</span>
              <h3>🔥 African Jazz Early Bird VIP Pass</h3>
              <p class="deal-host">Addis Music Festival Org</p>
              
              <div class="timer-box">
                <span>Ends in:</span>
                <strong class="timer-val">04d 18h 45m</strong>
              </div>

              <div class="deal-price-row">
                <div class="price-strikethrough-box">
                  <span class="orig-price">ETB 1,500</span>
                  <span class="discounted-price">ETB 1,050 / pass</span>
                </div>
                <button class="btn-gold" on:click={() => { selectedListing = { ...mockProviders[14], title: '🔥 African Jazz Early Bird VIP Pass', price: 1500, preAppliedPromo: 'ETHIO30' }; }}>
                  Book Deal →
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>

    {:else if activeTab === 'history'}
      <!-- Customer Passport Profile Dashboard -->
      <section class="passport-dashboard animate-fade-in">
        <div class="marketplace-card profile-summary-card">
          <div class="profile-main-meta">
            <div class="avatar-large">
              <Icon name="user" size={32} color="#ffffff" />
            </div>
            <div>
              <div class="user-title-row">
                <h2>{$currentUser ? $currentUser.name : 'Abebe Kebede'}</h2>
                <span class="badge-verified">✓ {$currentUser ? $currentUser.identityStatus : 'FAYDA VERIFIED'}</span>
              </div>
              <p class="user-sub-email">{$currentUser ? $currentUser.email : 'user1@aau.edu.et'}</p>
            </div>
          </div>

          <div class="spending-analytics-grid">
            <div class="spending-box">
              <span class="sp-lbl"><Icon name="bed" size={14} color="var(--accent-gold)" /> Hotels & Stays</span>
              <span class="sp-qty">{hotelBookings.length} Bookings</span>
              <span class="sp-val">ETB {hotelSpent.toLocaleString()}</span>
            </div>

            <div class="spending-box">
              <span class="sp-lbl"><Icon name="car" size={14} color="#3B82F6" /> Car Rentals</span>
              <span class="sp-qty">{carBookings.length} Rented</span>
              <span class="sp-val">ETB {carSpent.toLocaleString()}</span>
            </div>

            <div class="spending-box">
              <span class="sp-lbl"><Icon name="ticket" size={14} color="#E11D48" /> Events</span>
              <span class="sp-qty">{eventBookings.length} Passes</span>
              <span class="sp-val">ETB {eventSpent.toLocaleString()}</span>
            </div>

            <div class="spending-box">
              <span class="sp-lbl"><Icon name="bag" size={14} color="#10B981" /> Shop Products</span>
              <span class="sp-qty">{storeBookings.length} Items</span>
              <span class="sp-val">ETB {storeSpent.toLocaleString()}</span>
            </div>

            <div class="spending-box total-box">
              <span class="sp-lbl">Total Spending</span>
              <span class="sp-qty">All Services</span>
              <span class="sp-val total-val">ETB {totalSpentLifetime.toLocaleString()}</span>
            </div>
          </div>
        </div>

        <div class="profile-tabs-header">
          <button 
            class="profile-sub-tab {profileSubTab === 'BOOKINGS' ? 'active' : ''}" 
            on:click={() => profileSubTab = 'BOOKINGS'}>
            <Icon name="book" size={16} /> My Reservation Booklet ({$userBookings.length})
          </button>
          
          <button 
            class="profile-sub-tab {profileSubTab === 'FAVORITES' ? 'active' : ''}" 
            on:click={() => profileSubTab = 'FAVORITES'}>
            <Icon name="heart-filled" size={16} color="#ef4444" /> Saved Favorites ({favoritedListings.length})
          </button>
        </div>

        {#if profileSubTab === 'BOOKINGS'}
          <div class="passport-booklet-container">
            {#if $userBookings.length === 0}
              <div class="marketplace-card empty-state-box">
                <Icon name="calendar" size={36} color="var(--text-muted)" />
                <h3>Your reservation booklet is empty</h3>
                <p>Book stays, 4x4 vehicles, or cultural experiences across Ethiopia to fill your booklet!</p>
                <button class="btn-gold" on:click={() => activeTab = 'listings'}>Explore Marketplace</button>
              </div>
            {:else}
              <div class="booklet-pages-list">
                {#each $userBookings as b, idx}
                  <div 
                    class="passport-page-spread marketplace-card" 
                    role="button"
                    tabindex="0"
                    on:click={() => selectedBookingPass = b}
                    on:keydown={(e) => e.key === 'Enter' && (selectedBookingPass = b)}>
                    
                    <div class="page-left">
                      <div class="page-header-stamp">
                        <span class="stamp-number">PAGE 0{idx + 1}</span>
                        <span class={b.status === 'CONFIRMED' ? 'badge-verified' : 'badge-warning'}>
                          {b.status === 'CONFIRMED' ? '● ACTIVE TODAY' : 'REFUNDED'}
                        </span>
                      </div>

                      <img src={b.imageUrl} alt={b.listingTitle} class="page-thumb-img" />
                      
                      <div class="page-info-box">
                        <h3 class="page-title">{b.listingTitle}</h3>
                        <p class="page-location"><Icon name="mappin" size={13} color="var(--accent-gold)" /> {b.location}</p>
                        <p class="page-date">🗓️ Date: <strong>{b.bookingDate}</strong></p>
                      </div>
                    </div>

                    <div class="book-spine"></div>

                    <div class="page-right">
                      <div class="pass-stub-header">
                        <span class="stub-tag">ENTRY PASS STUB</span>
                        <span class="stub-tx">Tx: {b.id}</span>
                      </div>

                      <div class="mini-qr-preview">
                        <svg viewBox="0 0 100 100" width="70" height="70">
                          <rect x="0" y="0" width="30" height="30" fill="var(--text-main)" />
                          <rect x="5" y="5" width="20" height="20" fill="var(--bg-surface)" />
                          <rect x="10" y="10" width="10" height="10" fill="var(--text-main)" />

                          <rect x="70" y="0" width="30" height="30" fill="var(--text-main)" />
                          <rect x="75" y="5" width="20" height="20" fill="var(--bg-surface)" />
                          <rect x="80" y="10" width="10" height="10" fill="var(--text-main)" />

                          <rect x="0" y="70" width="30" height="30" fill="var(--text-main)" />
                          <rect x="5" y="75" width="20" height="20" fill="var(--bg-surface)" />
                          <rect x="10" y="80" width="10" height="10" fill="var(--text-main)" />
                          <rect x="40" y="40" width="20" height="20" fill="var(--accent-gold)" />
                        </svg>
                        <span class="tap-hint">Tap to Open Full Info</span>
                      </div>

                      <div class="stub-price-row">
                        <span class="qty-txt">Qty: {b.quantity}</span>
                        <span class="price-val">ETB {b.totalAmount.toLocaleString()}</span>
                      </div>

                      <button class="btn-gold open-pass-btn">
                        Open Booking Pass →
                      </button>
                    </div>
                  </div>
                {/each}
              </div>
            {/if}
          </div>

        {:else if profileSubTab === 'FAVORITES'}
          <div class="favorites-grid-container">
            {#if favoritedListings.length === 0}
              <div class="marketplace-card empty-state-box">
                <Icon name="heart" size={36} color="var(--text-muted)" />
                <h3>No saved favorites yet</h3>
                <p>Click the heart icon on any listing card to save your favorite stay, vehicle, or craft providers!</p>
                <button class="btn-gold" on:click={() => activeTab = 'listings'}>Browse Marketplace</button>
              </div>
            {:else}
              <div class="listings-grid">
                {#each favoritedListings as listing}
                  <ListingCard 
                    {listing} 
                    isFavorite={true} 
                    onToggleFavorite={toggleFavorite} 
                    onSelect={(l) => selectedProviderListing = l} 
                  />
                {/each}
              </div>
            {/if}
          </div>
        {/if}
      </section>

    {:else if activeTab === 'provider'}
      <!-- Provider Onboarding & Management Workspace -->
      <section class="provider-workspace animate-fade-in">
        {#if !isCompanyRegistered}
          <!-- Step 1: Provider Company Onboarding Form -->
          <div class="marketplace-card provider-onboard-card">
            <div class="onboard-header">
              <span class="badge-verified">✓ FAYDA VERIFIED PROVIDER NETWORK</span>
              <h2>🏢 Register Your Provider Business Company</h2>
              <p>Enter your business details to unlock your provider management dashboard and start publishing rooms, vehicles, or artisan items.</p>
            </div>

            <div class="form-grid">
              <div class="form-group">
                <label for="onboardCompanyName">Company / Business Name *</label>
                <input id="onboardCompanyName" type="text" placeholder="e.g. Kuriftu Resort & Spa" bind:value={companyName} class="input-field" />
              </div>

              <div class="form-group">
                <label for="onboardCategory">Primary Category *</label>
                <select id="onboardCategory" bind:value={companyCategory} class="input-field">
                  <option value="HOTEL">Hotels & Resorts</option>
                  <option value="CAR_RENTAL">Car Rentals & Fleets</option>
                  <option value="EVENT">Events & Passes</option>
                  <option value="STORE">Store Products & Crafts</option>
                </select>
              </div>

              <div class="form-group">
                <label for="onboardLocation">Location / City Spot *</label>
                <input id="onboardLocation" type="text" placeholder="e.g. Bishoftu Lakefront" bind:value={companyLocation} class="input-field" />
              </div>

              <div class="form-group">
                <label for="onboardPhone">Contact Phone *</label>
                <input id="onboardPhone" type="text" bind:value={companyPhone} class="input-field" />
              </div>
            </div>

            <button class="btn-gold onboard-btn" on:click={handleRegisterCompany} disabled={!companyName}>
              Register Company & Open Dashboard →
            </button>
          </div>
        {:else}
          <!-- Step 2: Active Provider Dashboard -->
          <div class="provider-dashboard-active">
            <div class="marketplace-card provider-dash-header">
              <div class="company-title-block">
                <div>
                  <span class="badge-verified">✓ ACTIVE PROVIDER</span>
                  <h2>{companyName}</h2>
                  <p class="company-sub"><Icon name="mappin" size={13} color="var(--accent-gold)" /> {companyLocation} • {companyCategory}</p>
                </div>
                
                <button class="btn-gold add-listing-btn" on:click={() => showAddListingModal = true}>
                  ➕ Add New Listing / Room / Variant
                </button>
              </div>

              <div class="provider-stats-row">
                <div class="stat-box">
                  <span class="stat-lbl">Active Options</span>
                  <span class="stat-val">3 Listings</span>
                </div>
                <div class="stat-box">
                  <span class="stat-lbl">Total Reservations</span>
                  <span class="stat-val">18 Bookings</span>
                </div>
                <div class="stat-box">
                  <span class="stat-lbl">Simulated Revenue</span>
                  <span class="stat-val">ETB 45,200</span>
                </div>
              </div>
            </div>

            {#if providerSuccessMsg}
              <div class="alert-success">{providerSuccessMsg}</div>
            {/if}

            <!-- Company Active Listings Table / Grid -->
            <div class="marketplace-card company-listings-card">
              <h3>Management Inventory ({companyName})</h3>
              <p class="subtext">Rooms, vehicle options, or items published by your business:</p>

              <div class="listings-table-wrapper">
                <table class="listings-table">
                  <thead>
                    <tr>
                      <th>Image</th>
                      <th>Listing Name</th>
                      <th>Category</th>
                      <th>Price</th>
                      <th>Available Stock</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr>
                      <td><img src={newImage} alt="Listing" class="table-thumb" /></td>
                      <td><strong>{companyName} — Executive Suite</strong></td>
                      <td>{companyCategory}</td>
                      <td>ETB {newPrice.toLocaleString()}</td>
                      <td><span class="stock-chip">{newCapacity} Available</span></td>
                      <td><span class="badge-verified">● PUBLISHED</span></td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        {/if}
      </section>
    {/if}
  </div>

  <!-- Provider Add Listing Modal -->
  {#if showAddListingModal}
    <div class="modal-backdrop" on:click|self={() => showAddListingModal = false} role="dialog" aria-modal="true">
      <div class="marketplace-card modal-content add-modal animate-fade-in">
        <div class="modal-header">
          <h2>➕ Add New Listing / Room Variant ({companyName})</h2>
          <button class="close-btn" on:click={() => showAddListingModal = false}>✕</button>
        </div>

        <div class="form-grid">
          <div class="form-group">
            <label for="modalListingTitle">Listing / Room / Option Name *</label>
            <input id="modalListingTitle" type="text" placeholder="e.g. Master Presidential Suite" bind:value={newTitle} class="input-field" />
          </div>

          <div class="form-group">
            <label for="modalListingType">Variant Type *</label>
            <select id="modalListingType" bind:value={newType} class="input-field">
              <option value="Single Deluxe Room">Single Deluxe Room</option>
              <option value="Master Presidential Suite">Master Presidential Suite</option>
              <option value="Family Villa Suite">Family Villa Suite</option>
              <option value="4x4 Land Cruiser">4x4 Land Cruiser</option>
              <option value="VIP Sedan">VIP Sedan</option>
              <option value="VIP Concert Pass">VIP Concert Pass</option>
              <option value="Craft Product">Craft Product</option>
            </select>
          </div>

          <div class="form-group">
            <label for="modalListingPrice">Price (ETB) *</label>
            <input id="modalListingPrice" type="number" bind:value={newPrice} class="input-field" />
          </div>

          <div class="form-group">
            <label for="modalListingStock">Available Stock / Quantity *</label>
            <input id="modalListingStock" type="number" bind:value={newCapacity} class="input-field" />
          </div>
        </div>

        <div class="form-group">
          <label for="modalListingImage">Picture Image URL</label>
          <input id="modalListingImage" type="text" bind:value={newImage} class="input-field" />
        </div>

        <div class="form-group">
          <label for="modalListingDesc">Description</label>
          <textarea id="modalListingDesc" rows="2" placeholder="Describe the room, vehicle, or product details..." bind:value={newDesc} class="input-field"></textarea>
        </div>

        <div class="modal-actions">
          <button class="btn-outline" on:click={() => showAddListingModal = false}>Cancel</button>
          <button class="btn-gold" id="btnPublishListing" on:click={handleCreateProviderListing} disabled={!newTitle}>
            Save & Publish Listing ✓
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Footer -->
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-col">
        <span class="brand">Easy<span class="brand-highlight">Service</span></span>
        <p class="footer-desc">Ethiopia's premier verified service marketplace connecting customers with quality hotels, car rentals, events, and shop products.</p>
      </div>

      <div class="footer-col">
        <h4>Ecosystem</h4>
        <ul>
          <li>7 Stays & Hotels</li>
          <li>7 Car Rentals</li>
          <li>7 Events & Passes</li>
          <li>7 Store Crafts</li>
        </ul>
      </div>

      <div class="footer-col">
        <h4>Destinations</h4>
        <ul>
          <li>Addis Ababa</li>
          <li>Bishoftu Lakefront</li>
          <li>Lake Hawassa Waterfront</li>
          <li>Lalibela Highlands</li>
        </ul>
      </div>
    </div>
    <div class="footer-bottom">
      © 2026 EasyService Marketplace Inc. All rights reserved.
    </div>
  </footer>

  <!-- Modals -->
  <BookingModal 
    listing={selectedListing} 
    onClose={() => selectedListing = null} 
    onBookingSuccess={(listingId, qty) => {
      listings = listings.map(l => l.id === listingId ? { ...l, availableQuantity: l.availableQuantity - qty } : l);
      if (selectedProviderListing && selectedProviderListing.id === listingId) {
        selectedProviderListing = { ...selectedProviderListing, availableQuantity: selectedProviderListing.availableQuantity - qty };
      }
    }} 
  />
  <BookingDetailsModal booking={selectedBookingPass} onClose={() => selectedBookingPass = null} onCancel={handleCancelBooking} />
  <SpinWheelModal show={showSpinModal} onClose={() => showSpinModal = false} />
  <EasyToolsModal show={showToolsModal} onClose={() => showToolsModal = false} />
  <RegisterModal show={showRegisterModal} onClose={() => showRegisterModal = false} />
  
  <LoginModal show={!$currentUser} />
</main>

<style>
  .app-container {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
    width: 100%;
    position: relative;
  }

  .category-transition-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(11, 19, 32, 0.85);
    backdrop-filter: blur(10px);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: center;
    pointer-events: none;
  }

  .moving-vehicle {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    color: #ffffff;
    font-weight: 800;
    font-size: 1.2rem;
    animation: vehiclePass 0.7s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  }

  @keyframes vehiclePass {
    0% { transform: translateX(-100vw) scale(0.8); opacity: 0; }
    50% { transform: translateX(0) scale(1.1); opacity: 1; }
    100% { transform: translateX(100vw) scale(0.8); opacity: 0; }
  }

  .main-content {
    width: 100%;
    margin: 0 auto;
    padding: 24px 32px 48px;
    flex-grow: 1;
  }

  .hero-section {
    position: relative;
    border-radius: var(--radius-xl);
    overflow: hidden;
    margin-bottom: 36px;
    padding: 56px 32px 40px;
    border: 1px solid var(--border-subtle);
    transition: all 0.4s ease;
    width: 100%;
  }

  .hero-bg-wrapper {
    position: absolute;
    inset: 0;
    z-index: 1;
  }

  .hero-bg-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: filter 0.4s ease, transform 0.6s ease;
  }

  .hero-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(11, 19, 32, 0.72) 0%, rgba(11, 19, 32, 0.92) 100%);
  }

  .hero-content-box {
    position: relative;
    z-index: 2;
    max-width: 980px;
    margin: 0 auto;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
  }

  .hero-pill-badge {
    background: rgba(200, 155, 60, 0.2);
    border: 1px solid var(--accent-gold);
    color: var(--accent-gold);
    font-size: 0.75rem;
    font-weight: 800;
    padding: 4px 14px;
    border-radius: 9999px;
    letter-spacing: 0.08em;
  }

  .hero-headline {
    font-size: 2.9rem;
    font-weight: 900;
    color: #ffffff;
    line-height: 1.15;
    letter-spacing: -0.02em;
  }

  .hero-subtext {
    font-size: 1.1rem;
    color: #E2DDD5;
    max-width: 680px;
  }

  .category-experience-tabs {
    display: flex;
    gap: 8px;
    margin-top: 12px;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(12px);
    padding: 6px;
    border-radius: 9999px;
    border: 1px solid rgba(255, 255, 255, 0.15);
  }

  .experience-tab {
    background: transparent;
    border: none;
    color: #E2DDD5;
    font-weight: 700;
    font-size: 0.88rem;
    padding: 8px 18px;
    border-radius: 9999px;
    cursor: pointer;
    transition: all 0.25s ease;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .experience-tab:hover, .experience-tab.active {
    background: var(--accent-gold);
    color: #FFFFFF;
    box-shadow: 0 4px 14px rgba(200, 155, 60, 0.4);
  }

  .search-module {
    margin-top: 16px;
    background: var(--bg-surface);
    padding: 16px 20px;
    display: flex;
    align-items: center;
    gap: 16px;
    width: 100%;
    box-shadow: 0 16px 36px rgba(0, 0, 0, 0.25);
    border-radius: var(--radius-xl);
  }

  .search-field {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    flex-grow: 1;
  }

  .search-field label {
    font-size: 0.7rem;
    font-weight: 800;
    color: var(--text-muted);
    text-transform: uppercase;
  }

  .input-with-icon {
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
  }

  .search-select, .search-text-input {
    background: transparent;
    border: none;
    font-size: 0.92rem;
    font-weight: 600;
    color: var(--text-main);
    width: 100%;
    outline: none;
  }

  .search-divider {
    width: 1px;
    height: 36px;
    background: var(--border-subtle);
  }

  .search-btn {
    padding: 12px 24px;
    font-size: 0.95rem;
    white-space: nowrap;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .location-chips-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-top: 10px;
    flex-wrap: wrap;
    justify-content: center;
  }

  .chips-label {
    font-size: 0.78rem;
    color: #9CA3AF;
    font-weight: 600;
  }

  .location-chip {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: #E2DDD5;
    font-size: 0.78rem;
    font-weight: 600;
    padding: 4px 12px;
    border-radius: 9999px;
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .location-chip:hover, .location-chip.active {
    background: var(--accent-gold);
    color: #fff;
    border-color: var(--accent-gold);
  }

  .discovery-curated-section {
    margin-bottom: 40px;
  }

  .section-header-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 20px;
  }

  .sub-pill {
    font-size: 0.7rem;
    font-weight: 800;
    color: var(--accent-gold);
    text-transform: uppercase;
    letter-spacing: 0.08em;
  }

  .section-title {
    font-size: 1.6rem;
    font-weight: 800;
    color: var(--text-main);
  }

  .see-all-btn {
    padding: 8px 16px;
    font-size: 0.85rem;
  }

  .trending-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
    gap: 24px;
  }

  .marketplace-browsing-section {
    display: grid;
    grid-template-columns: 260px 1fr;
    gap: 28px;
    margin-bottom: 48px;
  }

  .filters-sidebar {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 18px;
    height: fit-content;
  }

  .filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .filter-header h3 {
    font-size: 1rem;
    font-weight: 800;
  }

  .clear-btn {
    background: transparent;
    border: none;
    color: var(--accent-terracotta);
    font-size: 0.78rem;
    font-weight: 700;
    cursor: pointer;
  }

  .filter-label {
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--text-muted);
    text-transform: uppercase;
  }

  .listings-main-area {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .sort-header-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
  }

  .browse-title {
    font-size: 1.5rem;
    font-weight: 800;
    color: var(--text-main);
  }

  .results-count {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .sort-controls {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .sort-label {
    font-size: 0.8rem;
    font-weight: 600;
    color: var(--text-muted);
  }

  .sort-select {
    padding: 6px 12px;
    font-size: 0.85rem;
  }

  .listings-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
    gap: 24px;
  }

  .empty-state-box {
    padding: 48px;
    text-align: center;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }

  /* Customer Profile Analytics & Booklet */
  .passport-dashboard {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .profile-summary-card {
    padding: 24px 32px;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .profile-main-meta {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .avatar-large {
    width: 56px;
    height: 56px;
    background: var(--accent-gold);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .user-title-row {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .user-title-row h2 {
    font-size: 1.5rem;
    font-weight: 900;
    color: var(--text-main);
  }

  .user-sub-email {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .spending-analytics-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 14px;
  }

  .spending-box {
    background: var(--bg-surface-secondary);
    border: 1px solid var(--border-subtle);
    padding: 14px 16px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .spending-box.total-box {
    border-color: var(--accent-gold);
    background: var(--accent-gold-light);
  }

  .sp-lbl {
    font-size: 0.76rem;
    font-weight: 800;
    color: var(--text-muted);
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .sp-qty {
    font-size: 0.72rem;
    color: var(--text-muted);
  }

  .sp-val {
    font-size: 1.15rem;
    font-weight: 900;
    color: var(--text-main);
    margin-top: 2px;
  }

  .total-val {
    color: var(--accent-gold);
  }

  .profile-tabs-header {
    display: flex;
    gap: 12px;
    border-bottom: 2px solid var(--border-subtle);
    padding-bottom: 8px;
  }

  .profile-sub-tab {
    background: transparent;
    border: none;
    font-size: 0.95rem;
    font-weight: 800;
    color: var(--text-muted);
    padding: 8px 16px;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .profile-sub-tab:hover, .profile-sub-tab.active {
    color: var(--text-main);
    background: var(--bg-surface-secondary);
  }

  .booklet-pages-list {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .passport-page-spread {
    display: grid;
    grid-template-columns: 1fr 12px 1fr;
    cursor: pointer;
    position: relative;
    border: 2px solid var(--border-subtle);
    background: var(--bg-surface);
  }

  .passport-page-spread:hover {
    border-color: var(--accent-gold);
    transform: translateY(-4px);
  }

  .page-left {
    padding: 24px;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .page-header-stamp {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .stamp-number {
    font-size: 0.72rem;
    font-weight: 900;
    color: var(--accent-gold);
    letter-spacing: 0.08em;
  }

  .page-thumb-img {
    width: 100%;
    height: 140px;
    object-fit: cover;
    border-radius: var(--radius-md);
  }

  .page-title {
    font-size: 1.1rem;
    font-weight: 800;
  }

  .page-location {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .page-date {
    font-size: 0.82rem;
    color: var(--text-main);
  }

  .book-spine {
    background: linear-gradient(90deg, rgba(0, 0, 0, 0.15) 0%, rgba(0, 0, 0, 0.02) 50%, rgba(0, 0, 0, 0.15) 100%);
    border-left: 1px dashed var(--border-subtle);
    border-right: 1px dashed var(--border-subtle);
  }

  .page-right {
    padding: 24px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    background: var(--bg-surface-secondary);
    text-align: center;
  }

  .pass-stub-header {
    width: 100%;
    display: flex;
    justify-content: space-between;
    font-size: 0.72rem;
    font-weight: 800;
    color: var(--text-muted);
  }

  .mini-qr-preview {
    background: #ffffff;
    padding: 10px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    border: 1px solid var(--border-subtle);
    margin: 8px 0;
  }

  .tap-hint {
    font-size: 0.65rem;
    font-weight: 700;
    color: #111827;
  }

  .stub-price-row {
    display: flex;
    gap: 16px;
    align-items: center;
    font-size: 0.9rem;
  }

  .open-pass-btn {
    width: 100%;
    padding: 10px;
    font-size: 0.88rem;
  }

  /* Provider Management Styles */
  .provider-workspace {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .provider-onboard-card {
    padding: 32px;
    display: flex;
    flex-direction: column;
    gap: 20px;
    max-width: 680px;
    margin: 0 auto;
    width: 100%;
  }

  .onboard-header h2 {
    font-size: 1.5rem;
    font-weight: 900;
    color: var(--text-main);
    margin-top: 6px;
  }

  .onboard-header p {
    font-size: 0.9rem;
    color: var(--text-muted);
    margin-top: 4px;
  }

  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 16px;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .form-group label {
    font-size: 0.78rem;
    font-weight: 800;
    color: var(--text-muted);
  }

  .onboard-btn {
    padding: 12px 24px;
    font-size: 0.95rem;
  }

  .provider-dashboard-active {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .provider-dash-header {
    padding: 24px 32px;
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .company-title-block {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
  }

  .company-title-block h2 {
    font-size: 1.6rem;
    font-weight: 900;
    color: var(--text-main);
    margin-top: 4px;
  }

  .company-sub {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .add-listing-btn {
    padding: 10px 20px;
    font-size: 0.9rem;
  }

  .provider-stats-row {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 16px;
  }

  .stat-box {
    background: var(--bg-surface-secondary);
    border: 1px solid var(--border-subtle);
    padding: 14px 18px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .stat-lbl {
    font-size: 0.75rem;
    font-weight: 800;
    color: var(--text-muted);
  }

  .stat-val {
    font-size: 1.25rem;
    font-weight: 900;
    color: var(--text-main);
  }

  .company-listings-card {
    padding: 24px 32px;
  }

  .listings-table-wrapper {
    overflow-x: auto;
    margin-top: 14px;
  }

  .listings-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 0.88rem;
  }

  .listings-table th, .listings-table td {
    padding: 12px 16px;
    text-align: left;
    border-bottom: 1px solid var(--border-subtle);
  }

  .listings-table th {
    font-size: 0.75rem;
    font-weight: 800;
    color: var(--text-muted);
    text-transform: uppercase;
  }

  .table-thumb {
    width: 48px;
    height: 36px;
    object-fit: cover;
    border-radius: var(--radius-sm);
  }

  .stock-chip {
    background: rgba(16, 185, 129, 0.15);
    color: #10b981;
    font-weight: 800;
    padding: 2px 8px;
    border-radius: var(--radius-sm);
    font-size: 0.78rem;
  }

  .alert-success {
    background: var(--accent-gold-light);
    border: 1px solid var(--accent-gold);
    color: var(--text-main);
    padding: 12px 18px;
    border-radius: var(--radius-md);
    font-weight: 700;
    font-size: 0.88rem;
  }

  .add-modal {
    max-width: 560px;
    width: 90%;
  }

  .footer {
    background: var(--bg-surface);
    border-top: 1px solid var(--border-subtle);
    padding: 48px 32px 24px;
    margin-top: 40px;
    width: 100%;
  }

  .footer-container {
    width: 100%;
    margin: 0 auto;
    display: grid;
    grid-template-columns: 2fr 1fr 1fr;
    gap: 40px;
  }

  .footer-desc {
    font-size: 0.88rem;
    color: var(--text-muted);
    margin-top: 10px;
    max-width: 380px;
  }

  .footer-col h4 {
    font-size: 0.95rem;
    font-weight: 800;
    margin-bottom: 12px;
  }

  .footer-col ul {
    list-style: none;
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .footer-bottom {
    width: 100%;
    margin: 32px auto 0;
    padding-top: 20px;
    border-top: 1px solid var(--border-subtle);
    text-align: center;
    font-size: 0.8rem;
    color: var(--text-muted);
  }
  .hot-deals-section {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .deals-hero-banner {
    background: linear-gradient(135deg, var(--bg-surface-secondary) 0%, rgba(200, 155, 60, 0.12) 100%);
    border: 1px solid var(--border-subtle);
    padding: 32px;
    border-radius: var(--radius-xl);
    border-left: 4px solid var(--accent-terracotta);
  }

  .deals-hero-banner h2 {
    font-size: 1.8rem;
    font-weight: 900;
    margin-top: 4px;
  }

  .deals-hero-banner p {
    font-size: 0.95rem;
    color: var(--text-muted);
    margin-top: 6px;
  }

  .deals-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
    gap: 20px;
  }

  .deal-card {
    position: relative;
    overflow: hidden;
    display: flex;
    flex-direction: column;
  }

  .deal-badge-overlay {
    position: absolute;
    top: 14px;
    right: 14px;
    background: var(--accent-terracotta);
    color: #ffffff;
    font-weight: 900;
    font-size: 0.8rem;
    padding: 6px 12px;
    border-radius: var(--radius-sm);
    z-index: 10;
    box-shadow: 0 4px 12px rgba(184, 92, 56, 0.3);
  }

  .deal-img {
    width: 100%;
    height: 190px;
    object-fit: cover;
  }

  .deal-body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    flex-grow: 1;
  }

  .deal-category {
    font-size: 0.7rem;
    font-weight: 900;
    color: var(--accent-terracotta);
    letter-spacing: 0.08em;
  }

  .deal-body h3 {
    font-size: 1.15rem;
    font-weight: 900;
    color: var(--text-main);
  }

  .deal-host {
    font-size: 0.82rem;
    color: var(--text-muted);
  }

  .timer-box {
    background: var(--bg-surface-secondary);
    border: 1px solid var(--border-subtle);
    padding: 8px 12px;
    border-radius: var(--radius-sm);
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.8rem;
  }

  .timer-val {
    color: var(--accent-gold);
    font-weight: 900;
  }

  .deal-price-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-top: 10px;
    padding-top: 10px;
    border-top: 1px solid var(--border-subtle);
  }

  .price-strikethrough-box {
    display: flex;
    flex-direction: column;
  }

  .orig-price {
    font-size: 0.8rem;
    text-decoration: line-through;
    color: var(--text-muted);
  }

  .discounted-price {
    font-size: 1.18rem;
    font-weight: 900;
    color: var(--accent-gold);
  }
</style>
