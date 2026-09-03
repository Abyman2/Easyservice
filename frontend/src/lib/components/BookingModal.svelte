<script>
  import { currentUser, activePromoCodes } from '../stores/authStore.js';
  import { addBooking } from '../stores/bookingStore.js';
  import { createBooking } from '../api/api.js';
  import Icon from './Icon.svelte';

  export let listing = null;
  export let onClose = () => {};
  export let onBookingSuccess = (listingId, qty) => {};

  function handleCloseModal() {
    step = 1;
    currentListingId = null;
    bookingResult = null;
    onClose();
  }

  function getTodayStr() {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  function getTomorrowStr() {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  }

  let step = 1; // 1: Select, 2: Details, 3: Review, 4: Payment, 5: Confirmation
  let quantity = 1;
  let startDate = getTodayStr();
  let endDate = getTomorrowStr();
  let promoCode = listing?.preAppliedPromo || '';
  let paymentMethod = 'SIMULATED_BALANCE'; // 'SIMULATED_BALANCE' | 'TELEBIRR' | 'CBE' | 'CARD'
  let maskedCard = '**** **** **** 4821';
  let currentListingId = null;
  
  let customerName = $currentUser ? $currentUser.name : 'Abebe Kebede';
  let customerEmail = $currentUser ? $currentUser.email : 'abebe.kebede@aau.edu.et';
  let customerPhone = '+251 91 100 0001';
  let pickupTime = '09:00';
  let returnTime = '18:00';
  let driverOption = 'WITHOUT_DRIVER';

  let errorMsg = '';
  let loading = false;
  let bookingResult = null;

  // Reset stepper to Step 1 ONLY when switching to a completely different listing
  $: if (listing && listing.id !== currentListingId) {
    currentListingId = listing.id;
    step = 1;
    errorMsg = '';
    bookingResult = null;
    quantity = 1;
    startDate = getTodayStr();
    endDate = getTomorrowStr();
    pickupTime = '09:00';
    returnTime = '18:00';
    driverOption = 'WITHOUT_DRIVER';
    promoCode = listing?.preAppliedPromo || '';
  }

  $: cleanCode = promoCode.trim().toUpperCase();
  $: discountPercent = calculateDiscount(cleanCode, $activePromoCodes);
  $: isDurationCategory = listing && (listing.category === 'HOTEL' || listing.category === 'CAR_RENTAL');
  $: startLabel = listing?.category === 'HOTEL' ? 'Check-in Date' : (listing?.category === 'CAR_RENTAL' ? 'Pickup Date' : 'Booking Date');
  $: endLabel = listing?.category === 'HOTEL' ? 'Check-out Date' : (listing?.category === 'CAR_RENTAL' ? 'Return Date' : 'End Date');
  $: durationUnitLabel = listing?.category === 'HOTEL' ? 'night(s)' : (listing?.category === 'CAR_RENTAL' ? 'day(s)' : '');
  $: computedDays = isDurationCategory ? calculateDaysBetween(startDate, endDate) : 1;
  $: effectiveDuration = Math.max(1, computedDays);
  $: unitPrice = listing ? listing.price : 0;
  $: subtotal = unitPrice * quantity * effectiveDuration;
  $: discountAmount = Math.round((subtotal * discountPercent) / 100);
  $: serviceFee = Math.round(subtotal * 0.05); // 5% service fee
  $: totalAmount = Math.max(0, subtotal - discountAmount + serviceFee);
  $: remainingBalance = $currentUser ? Math.max(0, $currentUser.balance - totalAmount) : 0;
  $: isSoldOut = listing ? listing.availableQuantity <= 0 : false;

  function calculateDaysBetween(startStr, endStr) {
    if (!startStr || !endStr) return 1;
    const start = new Date(startStr);
    const end = new Date(endStr);
    const diffMs = end.getTime() - start.getTime();
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 1;
  }

  function calculateDiscount(code, registeredCodes) {
    if (!code) return 0;
    if (registeredCodes && registeredCodes[code] !== undefined) return registeredCodes[code];
    if (code.includes('30')) return 30;
    if (code.includes('25')) return 25;
    if (code.includes('20')) return 20;
    if (code.includes('15')) return 15;
    if (code.includes('10')) return 10;
    if (code.includes('5')) return 5;
    return 0;
  }

  function goToStep2() {
    errorMsg = '';
    if (isSoldOut) {
      errorMsg = 'BR-07 Violation: Service is fully booked/sold out. No inventory available.';
      return;
    }
    if (quantity <= 0) {
      errorMsg = 'BR-06 Violation: Booking quantity must be greater than zero.';
      return;
    }
    if (isDurationCategory && endDate <= startDate) {
      errorMsg = `${endLabel} must be after ${startLabel}.`;
      return;
    }
    if (quantity > listing.availableQuantity) {
      errorMsg = `BR-07 Violation: Requested quantity (${quantity}) exceeds available inventory (${listing.availableQuantity}).`;
      return;
    }
    step = 2;
  }

  function goToStep3() {
    errorMsg = '';
    if (!customerName || !customerEmail || !customerPhone) {
      errorMsg = 'Please complete all contact details.';
      return;
    }
    if (!$currentUser || !$currentUser.identityStatus || $currentUser.identityStatus === 'UNVERIFIED') {
      errorMsg = 'BR-02 Violation: Unverified accounts cannot initiate bookings. Please complete Fayda/Passport verification.';
      return;
    }
    step = 3; // Move to Review
  }

  function goToStep4() {
    step = 4; // Move to Payment
  }

  async function handleExecutePayment() {
    errorMsg = '';
    if (paymentMethod === 'SIMULATED_BALANCE' && $currentUser.balance < totalAmount) {
      errorMsg = `INSUFFICIENT FUNDS: Booking total (ETB ${totalAmount.toLocaleString()}) exceeds your EasyService Wallet balance (ETB ${$currentUser.balance.toLocaleString()}). Please choose another payment method or top up.`;
      return;
    }

    loading = true;
    try {
      const result = await createBooking($currentUser.id, listing.id, quantity, promoCode);
      if (result.status === 'CONFIRMED' || result.status === 'SUCCESS') {
        const finalPaid = totalAmount;
        if (paymentMethod === 'SIMULATED_BALANCE') {
          currentUser.update(u => ({ ...u, balance: Math.max(0, u.balance - finalPaid) }));
        }
        listing.availableQuantity -= quantity;
        onBookingSuccess(listing.id, quantity);

        const bookingId = 'ES-2026-' + Math.floor(100000 + Math.random() * 900000);

        bookingResult = {
          id: bookingId,
          listingId: listing.id,
          listingTitle: listing.title,
          category: listing.category,
          location: listing.location || 'Addis Ababa',
          hostName: listing.hostName || listing.title,
          imageUrl: listing.imageUrl || 'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=800&q=80',
          quantity: quantity,
          durationDays: effectiveDuration,
          durationUnitLabel: durationUnitLabel,
          startDate: startDate,
          endDate: endDate,
          pickupTime: listing.category === 'CAR_RENTAL' ? pickupTime : null,
          returnTime: listing.category === 'CAR_RENTAL' ? returnTime : null,
          driverOption: listing.category === 'CAR_RENTAL' ? driverOption : null,
          unitPrice: listing.price,
          subtotal: subtotal,
          discountAmount: discountAmount,
          serviceFee: serviceFee,
          totalAmount: finalPaid,
          paymentMethod: paymentMethod,
          status: 'CONFIRMED',
          providerStatus: 'PENDING',
          providerId: listing.providerId || listing.hostName || null,
          bookingDate: new Date().toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }),
          variantDetails: listing.variantDetails || null
        };

        addBooking(bookingResult);
        step = 5; // Move to confirmation
      } else {
        errorMsg = `DECLINED: Payment could not be completed with status ${result.status}`;
      }
    } catch (err) {
      errorMsg = `SERVICE UNAVAILABLE: ${err.message}`;
    } finally {
      loading = false;
    }
  }
</script>

{#if listing}
  <div class="modal-backdrop" on:click|self={handleCloseModal} role="dialog" aria-modal="true">
    <div class="marketplace-card modal-content animate-fade-in">
      <div class="modal-header">
        <div>
          <h2>EasyService Booking Engine</h2>
          <p class="subtitle">Complete your reservation safely</p>
        </div>
        <button class="close-btn" on:click={handleCloseModal}>✕</button>
      </div>

      <!-- Multi-step Progress Bar -->
      <div class="stepper-bar">
        <div class="step-item {step >= 1 ? 'active' : ''}">1 Select</div>
        <div class="step-line {step >= 2 ? 'active' : ''}"></div>
        <div class="step-item {step >= 2 ? 'active' : ''}">2 Details</div>
        <div class="step-line {step >= 3 ? 'active' : ''}"></div>
        <div class="step-item {step >= 3 ? 'active' : ''}">3 Review</div>
        <div class="step-line {step >= 4 ? 'active' : ''}"></div>
        <div class="step-item {step >= 4 ? 'active' : ''}">4 Payment</div>
        <div class="step-line {step >= 5 ? 'active' : ''}"></div>
        <div class="step-item {step >= 5 ? 'active' : ''}">5 Confirm</div>
      </div>

      <div class="listing-summary-box">
        <div class="summary-top">
          <h3>{listing.title}</h3>
          <span class="stock-badge {isSoldOut ? 'depleted' : ''}">
            {isSoldOut ? 'SOLD OUT' : `${listing.availableQuantity} available`}
          </span>
        </div>
        <p class="summary-loc"><Icon name="mappin" size={13} color="var(--accent-gold)" /> {listing.location || 'Addis Ababa, Ethiopia'}</p>
        {#if listing.variantDetails && listing.variantDetails.specs}
          <div class="specs-callout">✓ {listing.variantDetails.specs}</div>
        {/if}
      </div>

      {#if errorMsg}
        <div class="alert alert-error">{errorMsg}</div>
      {/if}

      <!-- STEP 1: SELECT QUANTITY, DATES & PROMO -->
      {#if step === 1}
        <div class="step-body">
          <div class="form-group">
            <label for="quantityInput">Quantity / Unit Count</label>
            <input id="quantityInput" type="number" min="1" max={listing.availableQuantity || 1} bind:value={quantity} disabled={isSoldOut} class="input-field" />
          </div>

          {#if isDurationCategory}
            <div class="date-picker-row animate-fade-in">
              <div class="form-group flex-1">
                <label for="startDateInput">{startLabel}</label>
                <input id="startDateInput" type="date" min={getTodayStr()} bind:value={startDate} disabled={isSoldOut} class="input-field" />
              </div>

              <div class="form-group flex-1">
                <label for="endDateInput">{endLabel}</label>
                <input id="endDateInput" type="date" min={startDate || getTodayStr()} bind:value={endDate} disabled={isSoldOut} class="input-field" />
              </div>
            </div>

            <div class="duration-badge-box">
              📅 Duration: <strong>{effectiveDuration} {durationUnitLabel}</strong> ({startDate} → {endDate})
            </div>

            {#if listing.category === 'CAR_RENTAL'}
              <div class="rental-options-grid animate-fade-in">
                <div class="form-group">
                  <label for="pickupTimeInput">Pickup Time</label>
                  <input id="pickupTimeInput" type="time" bind:value={pickupTime} disabled={isSoldOut} class="input-field" />
                </div>
                <div class="form-group">
                  <label for="returnTimeInput">Return Time</label>
                  <input id="returnTimeInput" type="time" bind:value={returnTime} disabled={isSoldOut} class="input-field" />
                </div>
              </div>
              <div class="driver-options" aria-label="Driver option">
                <span class="form-label">Driver Option</span>
                <label class="driver-option {driverOption === 'WITHOUT_DRIVER' ? 'selected' : ''}"><input type="radio" bind:group={driverOption} value="WITHOUT_DRIVER" /> Without driver</label>
                <label class="driver-option {driverOption === 'WITH_DRIVER' ? 'selected' : ''}"><input type="radio" bind:group={driverOption} value="WITH_DRIVER" /> With driver</label>
              </div>
            {/if}
          {:else}
            <div class="form-group animate-fade-in">
              <label for="singleDateInput">Preferred Booking Date</label>
              <input id="singleDateInput" type="date" min={getTodayStr()} bind:value={startDate} disabled={isSoldOut} class="input-field" />
            </div>
          {/if}

          <div class="form-group">
            <label for="promoCodeInput">Promotion Code (Optional)</label>
            <input id="promoCodeInput" type="text" placeholder="e.g. SUMMER20 (-20%)" bind:value={promoCode} disabled={isSoldOut} class="input-field" />
          </div>

          {#if discountPercent > 0}
            <div class="discount-applied-tag">
              ✓ Promo code applied: {discountPercent}% discount saved ETB {discountAmount.toLocaleString()}!
            </div>
          {/if}

          <div class="price-breakdown-box">
            <div class="breakdown-line">
              <span>Rate:</span>
              <strong>ETB {unitPrice.toLocaleString()} {isDurationCategory ? (listing.category === 'HOTEL' ? '/ night' : '/ day') : ''}</strong>
            </div>
            {#if isDurationCategory}
              <div class="breakdown-line">
                <span>Calculation:</span>
                <span>ETB {unitPrice.toLocaleString()} × {quantity} unit(s) × {effectiveDuration} {durationUnitLabel}</span>
              </div>
            {/if}
            <div class="breakdown-line total-line">
              <span>Total Payable (inc. 5% fee):</span>
              <span class="total-amount-display">ETB {totalAmount.toLocaleString()}</span>
            </div>
          </div>

          <div class="modal-actions">
            <button class="btn-outline" on:click={handleCloseModal}>Cancel</button>
            <button class="btn-gold" on:click={goToStep2} disabled={isSoldOut}>
              {isSoldOut ? 'Fully Booked' : 'Continue to Details →'}
            </button>
          </div>
        </div>

      <!-- STEP 2: CUSTOMER DETAILS -->
      {:else if step === 2}
        <div class="step-body">
          <div class="form-group">
            <label for="customerNameInput">Full Name</label>
            <input id="customerNameInput" type="text" bind:value={customerName} class="input-field" />
          </div>

          <div class="form-group">
            <label for="customerEmailInput">Email Address</label>
            <input id="customerEmailInput" type="email" bind:value={customerEmail} class="input-field" />
          </div>

          <div class="form-group">
            <label for="customerPhoneInput">Phone Number</label>
            <input id="customerPhoneInput" type="text" bind:value={customerPhone} class="input-field" />
          </div>

          <div class="verification-status-box">
            <Icon name="shield" size={16} color="var(--status-success-text)" />
            <span>Customer Identity: <strong>{$currentUser ? $currentUser.identityStatus : 'FAYDA VERIFIED'}</strong></span>
          </div>

          <div class="modal-actions">
            <button class="btn-outline" on:click={() => step = 1}>← Back</button>
            <button class="btn-gold" on:click={goToStep3}>Review Order →</button>
          </div>
        </div>

      <!-- STEP 3: REVIEW ORDER SUMMARY -->
      {:else if step === 3}
        <div class="step-body">
          <div class="review-box">
            <h4>📋 Reservation Order Review</h4>
            <div class="review-table">
              <div class="review-row"><span>Customer:</span> <strong>{customerName} ({$currentUser?.identityType || 'Fayda ID'})</strong></div>
              <div class="review-row"><span>Service Unit:</span> <strong>{listing.title}</strong></div>
              <div class="review-row"><span>Provider:</span> <strong>{listing.hostName || listing.title}</strong></div>
              <div class="review-row"><span>Quantity:</span> <strong>{quantity} unit(s)</strong></div>
              {#if isDurationCategory}
                <div class="review-row"><span>Dates ({startLabel} → {endLabel}):</span> <strong>{startDate} to {endDate} ({effectiveDuration} {durationUnitLabel})</strong></div>
                {#if listing.category === 'CAR_RENTAL'}
                  <div class="review-row"><span>Rental plan:</span> <strong>{pickupTime} pickup → {returnTime} return, {driverOption === 'WITH_DRIVER' ? 'with driver' : 'without driver'}</strong></div>
                {/if}
              {:else}
                <div class="review-row"><span>Booking Date:</span> <strong>{startDate}</strong></div>
              {/if}
              <div class="review-row"><span>Unit Price:</span> <span>ETB {unitPrice.toLocaleString()} {isDurationCategory ? (listing.category === 'HOTEL' ? '/ night' : '/ day') : ''}</span></div>
              <div class="review-row"><span>Subtotal:</span> <span>ETB {subtotal.toLocaleString()}</span></div>
              {#if discountAmount > 0}
                <div class="review-row discount"><span>Promo Discount ({discountPercent}%):</span> <span>- ETB {discountAmount.toLocaleString()}</span></div>
              {/if}
              <div class="review-row"><span>Service Fee (5%):</span> <span>+ ETB {serviceFee.toLocaleString()}</span></div>
              <div class="review-row total-row"><span>Final Total Amount:</span> <strong>ETB {totalAmount.toLocaleString()}</strong></div>
            </div>
          </div>

          <div class="cancellation-policy-box">
            <span>ℹ️ <strong>Cancellation Policy:</strong> Free cancellation up to 24 hours prior to scheduled date. Full instant refund to wallet.</span>
          </div>

          <div class="modal-actions">
            <button class="btn-outline" on:click={() => step = 2}>← Back</button>
            <button class="btn-gold" on:click={goToStep4}>Proceed to Payment →</button>
          </div>
        </div>

      <!-- STEP 4: SECURE PAYMENT METHODS -->
      {:else if step === 4}
        <div class="step-body">
          <h4 class="payment-title">Select Payment Method</h4>

          <div class="payment-options">
            <label class="payment-option-card {paymentMethod === 'SIMULATED_BALANCE' ? 'selected' : ''}">
              <input type="radio" name="payMethod" value="SIMULATED_BALANCE" bind:group={paymentMethod} />
              <div class="pay-info">
                <span class="pay-name">EasyService Balance Wallet</span>
                <span class="pay-sub">Available Balance: ETB {$currentUser ? $currentUser.balance.toLocaleString() : '0'}</span>
              </div>
            </label>

            <label class="payment-option-card {paymentMethod === 'TELEBIRR' ? 'selected' : ''}">
              <input type="radio" name="payMethod" value="TELEBIRR" bind:group={paymentMethod} />
              <div class="pay-info">
                <span class="pay-name">Telebirr Mobile Payment</span>
                <span class="pay-sub">Instant merchant push notification</span>
              </div>
            </label>

            <label class="payment-option-card {paymentMethod === 'CBE' ? 'selected' : ''}">
              <input type="radio" name="payMethod" value="CBE" bind:group={paymentMethod} />
              <div class="pay-info">
                <span class="pay-name">CBE Birr Account</span>
                <span class="pay-sub">Commercial Bank of Ethiopia Direct Transfer</span>
              </div>
            </label>

            <label class="payment-option-card {paymentMethod === 'CARD' ? 'selected' : ''}">
              <input type="radio" name="payMethod" value="CARD" bind:group={paymentMethod} />
              <div class="pay-info">
                <span class="pay-name">Credit / Debit Card (Masked)</span>
                <span class="pay-sub">Card ending in <strong>{maskedCard}</strong></span>
              </div>
            </label>
          </div>

          <div class="total-bar">
            <span>Total Charge:</span>
            <span class="total-val">ETB {totalAmount.toLocaleString()}</span>
          </div>

          <div class="modal-actions">
            <button class="btn-outline" on:click={() => step = 3}>← Back</button>
            <button class="btn-gold pay-btn" disabled={loading} on:click={handleExecutePayment}>
              {loading ? 'Processing Secure Payment...' : '🔒 Pay Securely Now →'}
            </button>
          </div>
        </div>

      <!-- STEP 5: CONFIRMATION -->
      {:else if step === 5}
        <div class="step-body confirmation-body text-center">
          <div class="success-icon-badge">✓</div>
          <h2>🎉 Booking Confirmed Successfully!</h2>
          <p class="confirm-id">Booking Reference ID: <strong>{bookingResult?.id}</strong></p>

          <div class="confirmation-card">
            <div class="confirm-item"><span>Service:</span> <strong>{bookingResult?.listingTitle}</strong></div>
            <div class="confirm-item"><span>Provider:</span> <strong>{bookingResult?.hostName}</strong></div>
            {#if isDurationCategory}
              <div class="confirm-item"><span>Reservation Dates:</span> <strong>{bookingResult?.startDate} → {bookingResult?.endDate} ({bookingResult?.durationDays} {bookingResult?.durationUnitLabel})</strong></div>
            {:else}
              <div class="confirm-item"><span>Booking Date:</span> <strong>{bookingResult?.startDate || bookingResult?.bookingDate}</strong></div>
            {/if}
            <div class="confirm-item"><span>Quantity:</span> <strong>{bookingResult?.quantity} unit(s)</strong></div>
            <div class="confirm-item"><span>Payment Method:</span> <strong>{bookingResult?.paymentMethod === 'SIMULATED_BALANCE' ? 'EasyService Wallet' : bookingResult?.paymentMethod}</strong></div>
            <div class="confirm-item total-paid-item"><span>Total Amount Paid:</span> <strong>ETB {bookingResult?.totalAmount?.toLocaleString()}</strong></div>
          </div>

          <div class="confirmation-note">
            <span>✓ Confirmation ticket saved to your Customer Profile.</span>
          </div>

          <div class="modal-actions confirm-actions">
            <button class="btn-gold" on:click={handleCloseModal}>Done & Back to Marketplace</button>
          </div>
        </div>
      {/if}
    </div>
  </div>
{/if}

<style>
  .modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background: rgba(0, 0, 0, 0.75);
    backdrop-filter: blur(8px);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
  }

  .modal-content {
    width: 92%;
    max-width: 680px;
    padding: 30px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    max-height: 90vh;
    overflow-y: auto;
  }

  .modal-header h2 {
    font-size: 1.35rem;
    font-weight: 900;
    color: var(--text-main);
  }

  .subtitle {
    font-size: 0.8rem;
    color: var(--text-muted);
  }

  .close-btn {
    background: transparent;
    border: none;
    font-size: 1.25rem;
    cursor: pointer;
    color: var(--text-muted);
  }

  .stepper-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--bg-surface-secondary);
    padding: 10px 14px;
    border-radius: var(--radius-md);
    font-size: 0.75rem;
    font-weight: 800;
  }

  .step-item {
    color: var(--text-muted);
  }

  .step-item.active {
    color: var(--accent-gold);
  }

  .step-line {
    flex: 1;
    height: 2px;
    background: var(--border-subtle);
    margin: 0 6px;
  }

  .step-line.active {
    background: var(--accent-gold);
  }

  .listing-summary-box {
    background: var(--bg-surface-secondary);
    padding: 12px 16px;
    border-radius: var(--radius-md);
    border-left: 3px solid var(--accent-gold);
  }

  .summary-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .summary-top h3 {
    font-size: 1.1rem;
    font-weight: 800;
  }

  .stock-badge {
    font-size: 0.72rem;
    font-weight: 800;
    color: var(--status-success-text);
  }

  .stock-badge.depleted {
    color: var(--accent-terracotta);
  }

  .summary-loc {
    font-size: 0.82rem;
    color: var(--text-muted);
  }

  .specs-callout {
    font-size: 0.78rem;
    font-weight: 700;
    color: var(--accent-gold);
    margin-top: 4px;
  }

  .step-body {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .form-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .date-picker-row {
    display: flex;
    gap: 12px;
    width: 100%;
  }

  .flex-1 {
    flex: 1;
  }

  .duration-badge-box {
    background: var(--accent-gold-light);
    border: 1px solid var(--accent-gold);
    color: var(--accent-gold);
    padding: 8px 12px;
    border-radius: var(--radius-sm);
    font-size: 0.82rem;
    font-weight: 700;
  }

  .rental-options-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .driver-options {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .form-label {
    width: 100%;
    color: var(--text-muted);
    font-size: .76rem;
    font-weight: 800;
  }

  .driver-option {
    display: flex;
    align-items: center;
    gap: 6px;
    border: 1px solid var(--border-subtle);
    border-radius: var(--radius-sm);
    padding: 8px 10px;
    color: var(--text-muted);
    font-size: .8rem;
    cursor: pointer;
  }

  .driver-option.selected {
    border-color: var(--accent-gold);
    background: var(--accent-gold-light);
    color: var(--text-main);
  }

  .driver-option input { accent-color: var(--accent-gold); }

  @media (max-width: 560px) {
    .date-picker-row, .rental-options-grid { grid-template-columns: 1fr; display: grid; }
  }

  .form-group label {
    font-size: 0.76rem;
    font-weight: 800;
    color: var(--text-muted);
  }

  .input-field {
    padding: 8px 12px;
    font-size: 0.9rem;
  }

  .discount-applied-tag {
    background: rgba(200, 155, 60, 0.15);
    color: var(--accent-gold);
    padding: 8px 12px;
    border-radius: var(--radius-sm);
    font-size: 0.82rem;
    font-weight: 700;
  }

  .price-summary-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-top: 10px;
    border-top: 1px solid var(--border-subtle);
    font-size: 0.9rem;
    font-weight: 800;
  }

  .price-breakdown-box {
    background: var(--bg-surface-secondary);
    padding: 12px 14px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 0.85rem;
    border: 1px solid var(--border-subtle);
  }

  .breakdown-line {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .breakdown-line.total-line {
    padding-top: 8px;
    margin-top: 4px;
    border-top: 1px dashed var(--border-subtle);
    font-size: 0.95rem;
    font-weight: 800;
  }

  .total-paid-item {
    font-size: 1.05rem;
    font-weight: 900;
    color: var(--accent-gold);
    padding-top: 6px;
    border-top: 1px dashed var(--border-subtle);
  }

  .confirmation-note {
    font-size: 0.8rem;
    color: var(--status-success-text);
    background: rgba(16, 185, 129, 0.08);
    padding: 8px 14px;
    border-radius: var(--radius-sm);
  }

  .total-amount-display {
    font-size: 1.3rem;
    font-weight: 900;
    color: var(--accent-gold);
  }

  .verification-status-box {
    display: flex;
    align-items: center;
    gap: 8px;
    background: var(--bg-surface-secondary);
    padding: 10px 14px;
    border-radius: var(--radius-sm);
    font-size: 0.82rem;
  }

  .review-box {
    background: var(--bg-surface-secondary);
    padding: 16px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .review-box h4 {
    font-size: 0.95rem;
    font-weight: 800;
  }

  .review-table {
    display: flex;
    flex-direction: column;
    gap: 6px;
    font-size: 0.85rem;
  }

  .review-row {
    display: flex;
    justify-content: space-between;
  }

  .review-row.discount {
    color: var(--accent-gold);
  }

  .review-row.total-row {
    padding-top: 8px;
    border-top: 1px dashed var(--border-subtle);
    font-size: 1.1rem;
    font-weight: 900;
    color: var(--accent-gold);
  }

  .cancellation-policy-box {
    background: rgba(16, 185, 129, 0.1);
    padding: 10px 12px;
    border-radius: var(--radius-sm);
    font-size: 0.78rem;
    color: var(--status-success-text);
  }

  .payment-title {
    font-size: 0.95rem;
    font-weight: 800;
  }

  .payment-options {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .payment-option-card {
    display: flex;
    align-items: center;
    gap: 12px;
    background: var(--bg-surface);
    border: 2px solid var(--border-subtle);
    padding: 12px 16px;
    border-radius: var(--radius-md);
    cursor: pointer;
  }

  .payment-option-card.selected {
    border-color: var(--accent-gold);
    background: var(--accent-gold-light);
  }

  .pay-info {
    display: flex;
    flex-direction: column;
  }

  .pay-name {
    font-size: 0.9rem;
    font-weight: 800;
  }

  .pay-sub {
    font-size: 0.75rem;
    color: var(--text-muted);
  }

  .total-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 1rem;
    font-weight: 800;
  }

  .total-val {
    font-size: 1.35rem;
    font-weight: 900;
    color: var(--accent-gold);
  }

  .pay-btn {
    padding: 12px 24px;
    font-size: 0.95rem;
  }

  .confirmation-body {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
  }

  .success-icon-badge {
    width: 60px;
    height: 60px;
    border-radius: 50%;
    background: var(--status-success-bg);
    color: var(--status-success-text);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 2rem;
    font-weight: 900;
  }

  .confirm-id {
    font-size: 0.85rem;
    color: var(--text-muted);
  }

  .confirmation-card {
    background: var(--bg-surface-secondary);
    padding: 16px;
    border-radius: var(--radius-md);
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
    font-size: 0.88rem;
  }

  .confirm-item {
    display: flex;
    justify-content: space-between;
  }

  .confirm-actions {
    margin-top: 10px;
  }

  .modal-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
</style>
