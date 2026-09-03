<script>
  import Icon from './Icon.svelte';

  export let listing;
  export let onSelect = () => {};
  export let isFavorite = false;
  export let onToggleFavorite = (id) => {};

  const imageMap = {
    list_1: 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80',
    list_2: 'https://images.unsplash.com/photo-1540555700478-4be289fbecef?auto=format&fit=crop&w=800&q=80',
    list_3: 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=800&q=80',
    list_4: 'https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?auto=format&fit=crop&w=800&q=80',
    list_5: 'https://images.unsplash.com/photo-1552519507-da3b142c6e3d?auto=format&fit=crop&w=800&q=80',
    list_6: 'https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?auto=format&fit=crop&w=800&q=80',
    list_7: 'https://images.unsplash.com/photo-1452626038306-9aae5e071dd3?auto=format&fit=crop&w=800&q=80',
    list_8: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=800&q=80',
    list_9: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80',
    list_10: 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80',
    list_11: 'https://images.unsplash.com/photo-1509631179647-0177331693ae?auto=format&fit=crop&w=800&q=80',
    list_12: 'https://images.unsplash.com/photo-1559056199-641a0ac8b55e?auto=format&fit=crop&w=800&q=80'
  };

  const categoryUnits = {
    HOTEL: '/ night',
    CAR_RENTAL: '/ day',
    EVENT: '/ ticket',
    STORE: ''
  };

  const categoryNames = {
    HOTEL: 'STAY',
    CAR_RENTAL: 'DRIVE',
    EVENT: 'EXPERIENCE',
    STORE: 'SHOP'
  };

  $: imageUrl = listing.imageUrl || imageMap[listing.id] || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80';
  $: unitText = categoryUnits[listing.category] || '';
  $: categoryTag = categoryNames[listing.category] || listing.category;
  $: locationDisplay = listing.location || 'Addis Ababa';
  $: isSoldOut = listing.availableQuantity <= 0;
</script>

<div
  class="marketplace-card listing-card animate-fade-in {isSoldOut ? 'sold-out' : ''}"
  role="button"
  tabindex="0"
  on:click={() => !isSoldOut && onSelect(listing)}
  on:keydown={(e) => e.key === 'Enter' && !isSoldOut && onSelect(listing)}>
  <!-- Card Image Area with Favorite Button & Category Tag -->
  <div class="card-image-wrapper">
    <img src={imageUrl} alt={listing.title} class="card-image" loading="lazy" />
    
    <span class="category-tag">
      <Icon name={listing.category} size={13} color="#ffffff" />
      {categoryTag}
    </span>

    <button 
      class="favorite-btn {isFavorite ? 'active' : ''}" 
      title={isFavorite ? 'Remove from favorites' : 'Save to favorites'}
      on:click|stopPropagation={() => onToggleFavorite(listing.id)}>
      <Icon name={isFavorite ? 'heart-filled' : 'heart'} size={17} color={isFavorite ? '#ef4444' : '#ffffff'} />
    </button>

    {#if isSoldOut}
      <div class="sold-out-overlay">
        <span>FULLY BOOKED</span>
      </div>
    {/if}
  </div>

  <div class="card-body">
    <!-- Location & Rating Row -->
    <div class="meta-top-row">
      <span class="location-text">
        <Icon name="mappin" size={13} color="var(--accent-gold)" />
        {locationDisplay}
      </span>
      <span class="rating-text">
        <Icon name="star" size={13} color="#f59e0b" />
        4.9 <span class="review-count">(128)</span>
      </span>
    </div>

    <h3 class="listing-title">{listing.title}</h3>
    <p class="listing-desc">{listing.description}</p>

    <!-- Verification & Availability Badge -->
    <div class="verification-row">
      <span class="badge-verified">
        <Icon name="shield" size={12} />
        Fayda Verified
      </span>
      <span class="availability-tag {isSoldOut ? 'depleted' : ''}">
        {isSoldOut ? '0 Remaining' : `${listing.availableQuantity} available`}
      </span>
    </div>

    <!-- Price Hierarchy & Action Button -->
    <div class="card-footer-row">
      <div class="price-box">
        <span class="from-label">From</span>
        <div class="price-display">
          <span class="price-val">ETB {listing.price ? listing.price.toLocaleString() : '0'}</span>
          <span class="unit-val">{unitText}</span>
        </div>
      </div>

      <button class="btn-gold details-btn" disabled={isSoldOut} on:click|stopPropagation={() => onSelect(listing)}>
        {isSoldOut ? 'Sold Out' : (
          listing.category === 'HOTEL' ? 'Explore Rooms & Rates →' :
          (listing.category === 'CAR_RENTAL' ? 'Explore Fleet & Rates →' :
          (listing.category === 'EVENT' ? 'Explore Passes & Tickets →' : 'Explore Crafts & Items →'))
        )}
      </button>
    </div>
  </div>
</div>

<style>
  .listing-card {
    display: flex;
    flex-direction: column;
    height: 100%;
    position: relative;
  }

  .listing-card.sold-out {
    opacity: 0.82;
  }

  .card-image-wrapper {
    position: relative;
    width: 100%;
    height: 210px;
    overflow: hidden;
    background: var(--bg-surface-secondary);
  }

  .card-image {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.5s ease;
  }

  .listing-card:hover .card-image {
    transform: scale(1.06);
  }

  .category-tag {
    position: absolute;
    top: 12px;
    left: 12px;
    background: rgba(11, 19, 32, 0.85);
    backdrop-filter: blur(8px);
    color: #ffffff;
    font-size: 0.68rem;
    font-weight: 800;
    padding: 4px 10px;
    border-radius: var(--radius-sm);
    display: flex;
    align-items: center;
    gap: 5px;
    letter-spacing: 0.08em;
  }

  .favorite-btn {
    position: absolute;
    top: 12px;
    right: 12px;
    background: rgba(11, 19, 32, 0.65);
    backdrop-filter: blur(8px);
    border: none;
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s ease;
    z-index: 10;
  }

  .favorite-btn:hover {
    background: rgba(11, 19, 32, 0.95);
    transform: scale(1.15);
  }

  .sold-out-overlay {
    position: absolute;
    inset: 0;
    background: rgba(17, 24, 39, 0.7);
    display: flex;
    justify-content: center;
    align-items: center;
    color: #ffffff;
    font-weight: 900;
    letter-spacing: 0.1em;
    font-size: 0.9rem;
  }

  .card-body {
    padding: 18px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    flex-grow: 1;
  }

  .meta-top-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 0.8rem;
  }

  .location-text {
    color: var(--text-muted);
    font-weight: 700;
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .rating-text {
    font-weight: 800;
    color: var(--text-main);
    display: flex;
    align-items: center;
    gap: 4px;
  }

  .review-count {
    color: var(--text-muted);
    font-weight: 500;
  }

  .listing-title {
    font-size: 1.15rem;
    font-weight: 800;
    color: var(--text-main);
    line-height: 1.35;
  }

  .listing-desc {
    font-size: 0.85rem;
    color: var(--text-muted);
    line-height: 1.45;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    flex-grow: 1;
  }

  .verification-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 2px;
  }

  .availability-tag {
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--status-success-text);
  }

  .availability-tag.depleted {
    color: var(--accent-terracotta);
  }

  .card-footer-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    padding-top: 12px;
    border-top: 1px solid var(--border-subtle);
    margin-top: 4px;
  }

  .from-label {
    font-size: 0.68rem;
    font-weight: 700;
    color: var(--text-muted);
    text-transform: uppercase;
    letter-spacing: 0.05em;
  }

  .price-display {
    display: flex;
    align-items: baseline;
    gap: 4px;
  }

  .price-val {
    font-size: 1.18rem;
    font-weight: 900;
    color: var(--accent-gold);
  }

  .unit-val {
    font-size: 0.78rem;
    color: var(--text-muted);
    font-weight: 600;
  }

  .details-btn {
    padding: 8px 16px;
    font-size: 0.85rem;
  }

  .details-btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
</style>
