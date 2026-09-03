<script>
  import { currentUser, demoUsers } from '../stores/authStore.js';
  import { userBookings } from '../stores/bookingStore.js';
  import Icon from './Icon.svelte';

  export let activeTab = 'listings';
  export let selectedCategory = 'ALL';
  export let currentTheme = 'light';
  export let toggleTheme = () => {};
  export let openSpinModal = () => {};
  export let openToolsModal = () => {};
  export let openRegisterModal = () => {};
  export let onSwitchUser = (user) => {};

  let showProfileDropdown = false;
  let showNotificationsDrawer = false;
  let customDepositAmount = 5000;

  $: bookingCount = $userBookings ? $userBookings.length : 0;

  // Smart notification system generated from active bookings
  $: notifications = ($userBookings || []).flatMap((b) => {
    const decisionNotification = b.providerStatus && b.providerStatus !== 'PENDING' ? [{
      id: `notif_decision_${b.id}`,
      title: b.providerStatus === 'ACCEPTED' ? 'Booking accepted' : 'Booking declined',
      message: b.providerDecisionMessage || `Your provider ${b.providerStatus.toLowerCase()} your booking.`,
      time: b.providerDecisionAt ? new Date(b.providerDecisionAt).toLocaleString() : 'Recently',
      unread: true,
      type: 'DECISION'
    }] : [];
    if (b.status !== 'CONFIRMED' && b.providerStatus !== 'DECLINED') return decisionNotification;
    return [
      ...decisionNotification,
      {
        id: `notif_day_${b.id}`,
        title: `URGENT: ${b.listingTitle}`,
        message: `Your reservation at ${b.location} is active TODAY! Tap to view your QR Entry Pass.`,
        time: 'Today at 08:00 AM',
        unread: true,
        type: 'URGENT'
      },
      {
        id: `notif_prev_${b.id}`,
        title: `Reminder: ${b.listingTitle}`,
        message: `Reminder sent 1 day before your scheduled booking. Get your travel details ready!`,
        time: 'Yesterday at 06:00 PM',
        unread: false,
        type: 'REMINDER'
      }
    ];
  });

  $: unreadNotifCount = notifications.filter(n => n.unread).length;

  function handleAddCustomBalance() {
    if ($currentUser && customDepositAmount > 0) {
      currentUser.update((u) => ({ ...u, balance: Number(u.balance || 0) + Number(customDepositAmount) }));
    }
  }

  const seededUsers = demoUsers;

  function handleSelectNavCategory(cat) {
    activeTab = 'listings';
    selectedCategory = cat;
  }

  function logout() {
    $currentUser = null;
    showProfileDropdown = false;
  }
</script>

<header class="navbar">
  <div class="nav-container">
    <!-- Brand Logo -->
    <div class="brand" role="button" tabindex="0" on:click={() => { activeTab = 'listings'; selectedCategory = 'ALL'; }} on:keydown={(e) => e.key === 'Enter' && (activeTab = 'listings')}>
      <img class="brand-logo" src={currentTheme === 'dark' ? '/easyservice-logo-dark.svg' : '/easyservice-logo.svg'} alt="EasyService, purely simple" />
    </div>

    <!-- Editorial Main Navigation Links with Professional Icons -->
    <nav class="nav-links">
      <button class="nav-item {activeTab === 'listings' && selectedCategory === 'ALL' ? 'active' : ''}" on:click={() => handleSelectNavCategory('ALL')}>
        <Icon name="sparkles" size={15} color="var(--accent-gold)" />
        Explore
      </button>
      <button class="nav-item {activeTab === 'listings' && selectedCategory === 'HOTEL' ? 'active' : ''}" on:click={() => handleSelectNavCategory('HOTEL')}>
        <Icon name="bed" size={15} />
        Stays
      </button>
      <button class="nav-item {activeTab === 'listings' && selectedCategory === 'CAR_RENTAL' ? 'active' : ''}" on:click={() => handleSelectNavCategory('CAR_RENTAL')}>
        <Icon name="car" size={15} />
        Drive
      </button>
      <button class="nav-item {activeTab === 'listings' && selectedCategory === 'EVENT' ? 'active' : ''}" on:click={() => handleSelectNavCategory('EVENT')}>
        <Icon name="ticket" size={15} />
        Experiences
      </button>
      <button class="nav-item {activeTab === 'listings' && selectedCategory === 'STORE' ? 'active' : ''}" on:click={() => handleSelectNavCategory('STORE')}>
        <Icon name="bag" size={15} />
        Shop
      </button>
      <button class="marketing-link deals-link {activeTab === 'deals' ? 'active' : ''}" on:click={() => activeTab = 'deals'}>
        <img src="/hot-deals-badge.svg" alt="" class="hot-deals-badge" />
        Hot Deals
      </button>
    </nav>

    <!-- Focused Utility Actions -->
    <div class="user-actions">
      <button class="marketing-action" title="Spin Lucky Promo Wheel" on:click={openSpinModal}>
        <Icon name="promo" size={15} color="currentColor" />
        <span>Spin the Wheel</span>
      </button>
      <button class="theme-toggle-btn" on:click={toggleTheme} title="Toggle Light/Dark Theme">
        <Icon name={currentTheme === 'light' ? 'moon' : 'sun'} size={18} color="var(--text-main)" />
      </button>

      <!-- User Profile & Persona Switcher -->
      {#if $currentUser}
        <div class="profile-dropdown-container">
          <button class="user-pill" on:click={() => showProfileDropdown = !showProfileDropdown}>
            <div class="wallet-badge-box">
              <span class="wallet-label">WALLETS</span>
              <span class="wallet-balance">ETB {$currentUser.balance ? $currentUser.balance.toLocaleString() : '0'}</span>
            </div>
            <span class="user-avatar"><Icon name="user" size={16} /></span>
          </button>

          {#if showProfileDropdown}
            <div class="profile-menu marketplace-card animate-fade-in">
              <div class="profile-intro">
                <span class="user-avatar profile-avatar"><Icon name="user" size={17} /></span>
                <div class="profile-copy">
                  <span class="user-display-name">{$currentUser.name}</span>
                  <span class="profile-type">{$currentUser.customerType || 'Customer'}</span>
                </div>
              </div>
              <span class="menu-email">{$currentUser.email}</span>
              <span class="badge-verified">✓ {$currentUser.identityStatus || 'Verified account'}</span>

              <div class="menu-divider"></div>
              <div class="account-links">
                <button class="menu-item" on:click={() => { activeTab = 'listings'; showProfileDropdown = false; }}><Icon name="user" size={15} /> My Profile</button>
                <button class="menu-item" on:click={() => { activeTab = 'history'; showProfileDropdown = false; }}><Icon name="book" size={15} /> My Bookings {#if bookingCount > 0}<span class="count-badge">{bookingCount}</span>{/if}</button>
                <button class="menu-item" on:click={() => { activeTab = 'history'; showProfileDropdown = false; }}><Icon name="book" size={15} /> Passport Profile</button>
                <button class="menu-item" on:click={() => { showProfileDropdown = false; }}><Icon name="bag" size={15} /> Easy Wallet</button>
                <button class="menu-item" on:click={() => { showNotificationsDrawer = true; showProfileDropdown = false; }}><Icon name="bell" size={15} /> Notifications {#if unreadNotifCount > 0}<span class="count-badge">{unreadNotifCount}</span>{/if}</button>
                <button class="menu-item" on:click={() => { openToolsModal(); showProfileDropdown = false; }}><Icon name="sparkles" size={15} /> Easy Tools</button>
                <button class="menu-item" on:click={() => { activeTab = 'provider'; showProfileDropdown = false; }}><Icon name="building" size={15} /> Provider Hub</button>
              </div>

              <!-- Custom Balance Top Up -->
              <div class="deposit-section">
                <label for="depositAmountInput" class="deposit-label">Easy Wallet</label>
                <span class="wallet-menu-balance">Available balance: ETB {$currentUser.balance ? $currentUser.balance.toLocaleString() : '0'}</span>
                <div class="deposit-input-row">
                  <input id="depositAmountInput" type="number" bind:value={customDepositAmount} class="input-field deposit-input" min="100" />
                  <button class="btn-gold deposit-add-btn" on:click={handleAddCustomBalance}>+ Add</button>
                </div>
              </div>

              <div class="menu-divider"></div>
              <div class="menu-subheader">Switch Active Account</div>
              {#each seededUsers as u}
                <button 
                  class="menu-item {$currentUser.id === u.id ? 'active' : ''}" 
                  on:click={() => { onSwitchUser(u); showProfileDropdown = false; }}>
                  <Icon name="user" size={14} color="var(--text-muted)" /> {u.name}
                </button>
              {/each}

              <div class="menu-divider"></div>
              <button class="menu-item" on:click={toggleTheme}><Icon name={currentTheme === 'light' ? 'moon' : 'sun'} size={15} /> {currentTheme === 'light' ? 'Dark Mode' : 'Light Mode'}</button>
              <button class="menu-item logout-item" on:click={logout}>
                <Icon name="user" size={15} /> Log Out
              </button>
            </div>
          {/if}

          {#if showNotificationsDrawer}
            <div class="notif-drawer marketplace-card animate-fade-in">
              <div class="drawer-header">
                <h3><Icon name="bell" size={15} color="var(--accent-gold)" /> Booking Reminders</h3>
                <button class="drawer-close" on:click={() => showNotificationsDrawer = false} aria-label="Close notifications">×</button>
              </div>
              {#if notifications.length === 0}
                <div class="notif-empty">No active notifications</div>
              {:else}
                <div class="notif-list">
                  {#each notifications as n}
                    <button class="notif-item {n.unread ? 'unread' : ''}" on:click={() => { activeTab = 'history'; showNotificationsDrawer = false; }}>
                      <span class="notif-title">{n.title}</span><span class="notif-msg">{n.message}</span><span class="notif-time">{n.time}</span>
                    </button>
                  {/each}
                </div>
              {/if}
            </div>
          {/if}
        </div>
      {:else}
        <button class="btn-gold" on:click={openRegisterModal}>Sign In</button>
      {/if}
    </div>
  </div>
</header>

<style>
  .navbar {
    background: var(--bg-surface);
    border-bottom: 1px solid var(--border-subtle);
    position: sticky;
    top: 0;
    z-index: 100;
    padding: 10px clamp(16px, 3vw, 40px);
    transition: background-color 0.25s ease, border-color 0.25s ease;
  }

  .nav-container {
    width: min(100%, 1560px);
    margin: 0 auto;
    display: flex;
    align-items: center;
    gap: clamp(16px, 2.5vw, 40px);
  }

  .brand {
    display: flex;
    flex-direction: column;
    cursor: pointer;
    flex: 0 0 auto;
    min-width: 218px;
    padding: 2px 0;
  }

  .brand-logo {
    display: block;
    width: 218px;
    height: auto;
    max-height: 54px;
    object-fit: contain;
    object-position: left center;
  }

  .brand-text {
    font-size: 1.4rem;
    font-weight: 900;
    letter-spacing: -0.02em;
    color: var(--text-main);
    line-height: 1;
  }

  .brand-highlight {
    color: var(--accent-gold);
  }

  .brand-tagline {
    font-size: 0.6rem;
    font-weight: 800;
    color: var(--text-muted);
    letter-spacing: 0.1em;
    margin-top: 2px;
  }

  .nav-links {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 2px;
    flex: 1 1 auto;
    min-width: 0;
  }

  .nav-item {
    background: transparent;
    border: none;
    color: var(--text-muted);
    font-weight: 700;
    font-size: 0.84rem;
    white-space: nowrap;
    padding: 9px 11px;
    border-radius: var(--radius-md);
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .nav-item:hover, .nav-item.active {
    color: var(--text-main);
    background: var(--bg-surface-secondary);
  }

  .marketing-link {
    margin-left: 6px;
    color: var(--accent-terracotta);
    font-weight: 800;
    padding-left: 5px;
  }

  .hot-deals-badge { width: 25px; height: 25px; object-fit: contain; }

  .marketing-link:hover,
  .marketing-link.active {
    color: var(--accent-terracotta-hover);
    background: color-mix(in srgb, var(--accent-terracotta) 10%, transparent);
  }

  .count-badge {
    background: var(--accent-gold);
    color: #fff;
    font-size: 0.7rem;
    font-weight: 800;
    padding: 2px 6px;
    border-radius: 9999px;
  }

  .user-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 7px;
    flex: 0 0 auto;
  }

  .marketing-action {
    display: inline-flex;
    align-items: center;
    gap: 7px;
    min-height: 40px;
    padding: 0 13px;
    border: 1px solid var(--accent-gold);
    border-radius: var(--radius-md);
    background: var(--accent-gold-light);
    color: var(--accent-gold-hover);
    font-size: 0.78rem;
    font-weight: 800;
    white-space: nowrap;
    cursor: pointer;
    transition: background-color 0.2s ease, color 0.2s ease, transform 0.2s ease;
  }

  .marketing-action:hover {
    background: var(--accent-gold);
    color: #fff;
    transform: translateY(-1px);
  }

  .notification-container {
    position: relative;
  }

  .notif-bell-btn {
    background: var(--bg-surface-secondary);
    border: 1px solid var(--border-subtle);
    padding: 8px;
    border-radius: var(--radius-md);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
  }

  .notif-badge {
    position: absolute;
    top: -4px;
    right: -4px;
    background: #ef4444;
    color: #ffffff;
    font-size: 0.65rem;
    font-weight: 900;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .notif-drawer {
    position: absolute;
    top: calc(100% + 10px);
    right: 0;
    width: 320px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    z-index: 200;
  }

  .drawer-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid var(--border-subtle);
    padding-bottom: 8px;
  }

  .drawer-header h3 {
    font-size: 0.92rem;
    font-weight: 800;
    display: flex;
    align-items: center;
    gap: 6px;
  }

  .sub-text {
    font-size: 0.7rem;
    color: var(--text-muted);
  }

  .notif-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 280px;
    overflow-y: auto;
  }

  .notif-item {
    background: var(--bg-surface-secondary);
    padding: 10px;
    border-radius: var(--radius-md);
    cursor: pointer;
    display: flex;
    flex-direction: column;
    gap: 4px;
    transition: all 0.2s ease;
  }

  .notif-item:hover {
    border-left: 3px solid var(--accent-gold);
  }

  .notif-title {
    font-size: 0.82rem;
    font-weight: 800;
    color: var(--text-main);
  }

  .notif-msg {
    font-size: 0.76rem;
    color: var(--text-muted);
    line-height: 1.35;
  }

  .notif-time {
    font-size: 0.68rem;
    color: var(--accent-gold);
    font-weight: 700;
  }

  .notif-empty {
    padding: 16px;
    text-align: center;
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .promo-wheel-btn {
    background: var(--accent-gold-light);
    border: 1px solid var(--accent-gold);
    color: var(--accent-gold);
    font-weight: 700;
    font-size: 0.82rem;
    min-height: 40px;
    padding: 7px 11px;
    border-radius: var(--radius-md);
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
    transition: all 0.2s ease;
  }

  .promo-wheel-btn:hover {
    background: var(--accent-gold);
    color: #fff;
  }

  .theme-toggle-btn {
    background: var(--bg-surface-secondary);
    border: 1px solid var(--border-subtle);
    width: 40px;
    height: 40px;
    padding: 0;
    border-radius: var(--radius-md);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
  }

  .theme-toggle-btn:hover {
    border-color: var(--accent-gold);
  }

  .profile-dropdown-container {
    position: relative;
  }

  .user-pill {
    background: var(--bg-surface-secondary);
    border: 1px solid var(--border-subtle);
    min-height: 42px;
    padding: 4px 6px 4px 11px;
    border-radius: 9999px;
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
  }

  .user-pill:hover {
    border-color: var(--accent-gold);
  }

  .wallet-badge-box {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }

  .wallet-label {
    font-size: 0.6rem;
    font-weight: 800;
    color: var(--text-muted);
    letter-spacing: 0.05em;
  }

  .wallet-balance {
    font-size: 0.85rem;
    font-weight: 800;
    color: var(--status-success-text);
  }

  .user-avatar {
    background: var(--accent-gold);
    color: #fff;
    width: 30px;
    height: 30px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .profile-menu {
    position: absolute;
    top: calc(100% + 8px);
    right: 0;
    width: 290px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    z-index: 200;
  }

  .profile-intro {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .profile-avatar {
    flex: 0 0 auto;
  }

  .profile-copy {
    display: flex;
    flex-direction: column;
    gap: 1px;
    min-width: 0;
  }

  .profile-type {
    color: var(--text-muted);
    font-size: 0.72rem;
    font-weight: 700;
    text-transform: capitalize;
  }

  .account-links {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 2px;
  }

  .account-links .menu-item {
    min-width: 0;
    font-size: 0.78rem;
    padding: 8px 6px;
  }

  .wallet-menu-balance {
    color: var(--status-success-text);
    font-size: 0.76rem;
    font-weight: 800;
  }

  .menu-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .user-display-name {
    font-size: 0.95rem;
    font-weight: 800;
    color: var(--text-main);
  }

  .menu-email {
    font-size: 0.78rem;
    color: var(--text-muted);
  }

  .deposit-section {
    margin-top: 8px;
    background: var(--bg-surface-secondary);
    padding: 10px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .deposit-label {
    font-size: 0.7rem;
    font-weight: 800;
    color: var(--text-muted);
    text-transform: uppercase;
  }

  .deposit-input-row {
    display: flex;
    gap: 6px;
  }

  .deposit-input {
    padding: 6px 10px;
    font-size: 0.85rem;
    width: 100%;
  }

  .deposit-add-btn {
    padding: 6px 12px;
    font-size: 0.8rem;
  }

  .menu-divider {
    height: 1px;
    background: var(--border-subtle);
    margin: 4px 0;
  }

  .menu-subheader {
    font-size: 0.7rem;
    font-weight: 800;
    color: var(--text-muted);
    text-transform: uppercase;
  }

  .menu-item {
    background: transparent;
    border: none;
    color: var(--text-main);
    padding: 8px 10px;
    border-radius: var(--radius-sm);
    text-align: left;
    font-size: 0.85rem;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .menu-item:hover, .menu-item.active {
    background: var(--accent-gold-light);
    color: var(--accent-gold);
  }

  .logout-item {
    color: #ef4444;
  }

  @media (max-width: 1320px) {
    .nav-container {
      gap: 14px;
    }

    .brand {
      min-width: 150px;
    }

    .nav-item {
      padding-inline: 8px;
    }

    .user-actions {
      gap: 5px;
    }

    .promo-wheel-btn {
      padding-inline: 8px;
    }

    .marketing-action {
      padding-inline: 9px;
    }
  }

  @media (max-width: 1120px) {
    .nav-links .nav-item:nth-child(4),
    .user-actions > .nav-item:last-of-type {
      display: none;
    }

    .marketing-action span {
      display: none;
    }

    .marketing-action {
      width: 40px;
      justify-content: center;
      padding-inline: 0;
    }
  }

  @media (max-width: 820px) {
    .navbar {
      padding-block: 8px;
    }

    .nav-container {
      flex-wrap: wrap;
      gap: 8px 14px;
    }

    .brand {
      min-width: 0;
      margin-right: auto;
    }

    .brand-text {
      font-size: 1.25rem;
    }

    .brand-logo {
      width: 176px;
      max-height: 46px;
    }

    .nav-links {
      order: 3;
      flex-basis: 100%;
      justify-content: flex-start;
      overflow-x: auto;
      scrollbar-width: none;
      border-top: 1px solid var(--border-subtle);
      padding-top: 7px;
    }

    .nav-links::-webkit-scrollbar {
      display: none;
    }

    .nav-links .nav-item:nth-child(4),
    .user-actions > .nav-item:last-of-type {
      display: flex;
    }

    .marketing-link {
      margin-left: 0;
    }

    .hot-deals-badge { width: 23px; height: 23px; }

    .user-actions .nav-item span:not(.count-badge) {
      display: none;
    }

    .user-actions .nav-item {
      width: 40px;
      height: 40px;
      justify-content: center;
      padding: 0;
    }

    .wallet-badge-box {
      display: none;
    }
  }

  @media (max-width: 520px) {
    .navbar {
      padding-inline: 12px;
    }

    .user-actions {
      gap: 3px;
    }

    .user-pill,
    .theme-toggle-btn,
    .marketing-action,
    .user-actions .nav-item,
    .notif-bell-btn {
      width: 36px;
      min-height: 36px;
      height: 36px;
    }

    .user-avatar {
      width: 27px;
      height: 27px;
    }

    .nav-links .nav-item {
      padding-inline: 9px;
      font-size: 0.8rem;
    }
  }

  @media (max-width: 600px) {
    .nav-container { gap: 6px; }
    .brand-logo { width: 150px; max-height: 40px; }
    .marketing-link { display: none; }
    .nav-links { gap: 0; }
    .nav-links .nav-item { padding-inline: 10px; }
    .nav-links .nav-item:nth-child(4),
    .nav-links .nav-item:nth-child(5) { display: none; }
    .marketing-action { display: none; }
    .profile-menu { position: fixed; top: 64px; right: 12px; left: 12px; width: auto; max-height: calc(100vh - 78px); overflow-y: auto; }
  }
</style>
