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
    if (b.status !== 'CONFIRMED') return [];
    return [
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
      <span class="brand-text">Easy<span class="brand-highlight">Service</span></span>
      <span class="brand-tagline">PURELY SIMPLE</span>
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
      <button class="nav-item {activeTab === 'deals' ? 'active' : ''}" on:click={() => { activeTab = 'deals'; }}>
        <Icon name="sparkles" size={15} color="var(--accent-terracotta)" />
        🔥 Hot Deals
      </button>
    </nav>

    <!-- Right Side Actions -->
    <div class="user-actions">
      <!-- Spin Wheel Promos -->
      <button class="promo-wheel-btn" title="Spin Lucky Promo Wheel" on:click={openSpinModal}>
        <Icon name="promo" size={16} color="var(--accent-gold)" />
        <span class="btn-text">Spin Wheel</span>
      </button>

      <button class="promo-wheel-btn" title="Easy Tools: Split Bill" on:click={openToolsModal}>
        <Icon name="sparkles" size={16} color="var(--accent-gold)" />
        <span class="btn-text">Easy Tools</span>
      </button>

      <!-- Smart Notification Reminders System -->
      <div class="notification-container">
        <button class="notif-bell-btn" title="Booking Notifications" on:click={() => showNotificationsDrawer = !showNotificationsDrawer}>
          <Icon name="bell" size={18} color="var(--text-main)" />
          {#if unreadNotifCount > 0}
            <span class="notif-badge">{unreadNotifCount}</span>
          {/if}
        </button>

        {#if showNotificationsDrawer}
          <div class="notif-drawer marketplace-card animate-fade-in">
            <div class="drawer-header">
              <h3><Icon name="bell" size={15} color="var(--accent-gold)" /> Booking Reminders</h3>
              <span class="sub-text">{notifications.length} Notifications</span>
            </div>

            {#if notifications.length === 0}
              <div class="notif-empty">No active notifications</div>
            {:else}
              <div class="notif-list">
                {#each notifications as n}
                  <div class="notif-item {n.unread ? 'unread' : ''}" on:click={() => { activeTab = 'history'; showNotificationsDrawer = false; }}>
                    <span class="notif-title">{n.title}</span>
                    <p class="notif-msg">{n.message}</p>
                    <span class="notif-time">{n.time}</span>
                  </div>
                {/each}
              </div>
            {/if}
          </div>
        {/if}
      </div>

      <!-- My Bookings Link -->
      <button class="nav-item {activeTab === 'history' ? 'active' : ''}" on:click={() => activeTab = 'history'}>
        <Icon name="book" size={16} color="var(--accent-gold)" />
        <span>Passport Profile</span>
        {#if bookingCount > 0}
          <span class="count-badge">{bookingCount}</span>
        {/if}
      </button>

      <!-- Become a Provider -->
      <button class="nav-item {activeTab === 'provider' ? 'active' : ''}" on:click={() => activeTab = 'provider'}>
        Become Provider
      </button>

      <!-- Theme Switcher Toggle (Light / Dark) -->
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
              <div class="menu-header">
                <span class="user-display-name">{$currentUser.name}</span>
                <span class="badge-verified">✓ {$currentUser.identityStatus}</span>
              </div>
              <span class="menu-email">{$currentUser.email}</span>

              <!-- Custom Balance Top Up -->
              <div class="deposit-section">
                <label for="depositAmountInput" class="deposit-label">Easy Wallet Top Up</label>
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
              <button class="menu-item logout-item" on:click={logout}>
                Log Out
              </button>
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
    padding: 12px 32px;
    transition: background-color 0.25s ease, border-color 0.25s ease;
  }

  .nav-container {
    width: 100%;
    margin: 0 auto;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .brand {
    display: flex;
    flex-direction: column;
    cursor: pointer;
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
    gap: 6px;
  }

  .nav-item {
    background: transparent;
    border: none;
    color: var(--text-muted);
    font-weight: 700;
    font-size: 0.88rem;
    padding: 8px 14px;
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
    gap: 12px;
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
    padding: 7px 12px;
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
    padding: 8px;
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
    padding: 4px 6px 4px 12px;
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
</style>
