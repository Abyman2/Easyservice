<script>
  import { splitBillEqual, spinPayerWheel } from '../api/api.js';
  import Icon from './Icon.svelte';

  export let show = false;
  export let onClose = () => {};

  let activeMode = 'QUICK'; // 'QUICK' | 'ITEMIZED' | 'WHEEL'

  // 1. Quick Split State
  let totalAmount = 3000;
  let peopleCount = 4;
  let quickSplitResult = null;

  // 2. Itemized Receipt Split State
  let receiptItems = [
    { id: 1, name: 'Special Gourmet Pizza', price: 800, assignedTo: ['Abel', 'Sara'] },
    { id: 2, name: 'Ethiopian Beef Burger', price: 450, assignedTo: ['John'] },
    { id: 3, name: 'Fresh Juice & Soft Drinks', price: 300, assignedTo: ['Abel', 'Mimi'] },
    { id: 4, name: 'Crispy French Fries', price: 250, assignedTo: ['Abel', 'Sara', 'John', 'Mimi'] }
  ];
  let participants = ['Abel', 'Sara', 'John', 'Mimi'];

  $: personTotals = participants.reduce((acc, person) => {
    let sum = 0;
    receiptItems.forEach((item) => {
      if (item.assignedTo.includes(person)) {
        sum += item.price / item.assignedTo.length;
      }
    });
    acc[person] = sum;
    return acc;
  }, {});

  // 3. Spin Payer Wheel State
  let wheelNames = 'Abel, Sara, John, Mimi';
  let isSpinning = false;
  let selectedPayer = null;

  function handleCalculateQuick() {
    quickSplitResult = (totalAmount / Math.max(1, peopleCount)).toFixed(2);
  }

  function toggleItemAssignment(item, person) {
    if (item.assignedTo.includes(person)) {
      if (item.assignedTo.length > 1) {
        item.assignedTo = item.assignedTo.filter(p => p !== person);
      }
    } else {
      item.assignedTo = [...item.assignedTo, person];
    }
    receiptItems = [...receiptItems];
  }

  function handleSpinWheel() {
    const list = wheelNames.split(',').map(s => s.trim()).filter(Boolean);
    if (list.length === 0) return;

    isSpinning = true;
    selectedPayer = null;

    setTimeout(() => {
      selectedPayer = list[Math.floor(Math.random() * list.length)];
      isSpinning = false;
    }, 1500);
  }
</script>

{#if show}
  <div class="modal-backdrop" on:click|self={onClose} role="dialog" aria-modal="true">
    <div class="marketplace-card modal-content animate-fade-in">
      <div class="modal-header">
        <div>
          <h2>💳 Easy Tools — Split Bill & Payer Wheel</h2>
          <p class="subtitle">Quick bill calculation, receipt item breakdown, and random payer wheel</p>
        </div>
        <button class="close-btn" on:click={onClose}>✕</button>
      </div>

      <!-- Mode Selector Tabs -->
      <div class="mode-tabs">
        <button class="mode-tab {activeMode === 'QUICK' ? 'active' : ''}" on:click={() => activeMode = 'QUICK'}>
          ⚡ Quick Split
        </button>
        <button class="mode-tab {activeMode === 'ITEMIZED' ? 'active' : ''}" on:click={() => activeMode = 'ITEMIZED'}>
          🧾 Split by Items
        </button>
        <button class="mode-tab {activeMode === 'WHEEL' ? 'active' : ''}" on:click={() => activeMode = 'WHEEL'}>
          🎡 Spin the Wheel
        </button>
      </div>

      <!-- Mode 1: Quick Split -->
      {#if activeMode === 'QUICK'}
        <div class="tool-pane">
          <div class="form-grid">
            <div class="form-field">
              <label for="totalBillInput">Total Bill Amount (ETB)</label>
              <input id="totalBillInput" type="number" bind:value={totalAmount} class="input-field" />
            </div>

            <div class="form-field">
              <label for="peopleCountInput">Number of People</label>
              <input id="peopleCountInput" type="number" min="1" max="50" bind:value={peopleCount} class="input-field" />
            </div>
          </div>

          <button class="btn-gold calc-btn" on:click={handleCalculateQuick}>
            Calculate Per Person Share
          </button>

          {#if quickSplitResult !== null}
            <div class="result-card">
              <span class="result-label">Each Person Pays:</span>
              <strong class="result-amount">ETB {Number(quickSplitResult).toLocaleString()}</strong>
            </div>
          {/if}
        </div>

      <!-- Mode 2: Split by Items -->
      {:else if activeMode === 'ITEMIZED'}
        <div class="tool-pane">
          <p class="pane-desc">Tap names on each scanned item to divide the bill according to who consumed what:</p>

          <div class="items-list">
            {#each receiptItems as item}
              <div class="receipt-item-card">
                <div class="item-meta">
                  <span class="item-name">{item.name}</span>
                  <span class="item-price">ETB {item.price.toLocaleString()}</span>
                </div>

                <div class="consumers-row">
                  {#each participants as p}
                    <button 
                      class="consumer-chip {item.assignedTo.includes(p) ? 'selected' : ''}"
                      on:click={() => toggleItemAssignment(item, p)}>
                      {p} {item.assignedTo.includes(p) ? '✓' : ''}
                    </button>
                  {/each}
                </div>
              </div>
            {/each}
          </div>

          <div class="summary-breakdown">
            <h4>Individual Shares Breakdown</h4>
            <div class="shares-grid">
              {#each participants as p}
                <div class="share-box">
                  <span class="person-name">{p}</span>
                  <span class="person-share">ETB {Math.round(personTotals[p]).toLocaleString()}</span>
                </div>
              {/each}
            </div>
          </div>
        </div>

      <!-- Mode 3: Spin the Payer Wheel -->
      {:else if activeMode === 'WHEEL'}
        <div class="tool-pane text-center">
          <div class="form-field">
            <label for="wheelParticipantsInput">Participant Names (comma separated)</label>
            <input id="wheelParticipantsInput" type="text" bind:value={wheelNames} class="input-field" />
          </div>

          <div class="wheel-stage">
            <div class="wheel-spinner {isSpinning ? 'spinning' : ''}">
              <span class="wheel-center-icon">🎯</span>
            </div>

            <button class="btn-gold spin-act-btn" disabled={isSpinning} on:click={handleSpinWheel}>
              {isSpinning ? 'Spinning Wheel...' : 'SPIN THE WHEEL! 🎡'}
            </button>
          </div>

          {#if selectedPayer}
            <div class="winner-banner animate-fade-in">
              <span class="winner-title">🎉 WINNER ANNOUNCED 🎉</span>
              <h3 class="winner-name">{selectedPayer} PAYS THE BILL!</h3>
            </div>
          {/if}
        </div>
      {/if}

      <div class="modal-footer">
        <button class="btn-outline" on:click={onClose}>Close</button>
      </div>
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
    max-width: 650px;
    padding: 28px;
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
    font-size: 0.82rem;
    color: var(--text-muted);
  }

  .close-btn {
    background: transparent;
    border: none;
    font-size: 1.25rem;
    cursor: pointer;
    color: var(--text-muted);
  }

  .mode-tabs {
    display: flex;
    gap: 8px;
    background: var(--bg-surface-secondary);
    padding: 6px;
    border-radius: var(--radius-md);
  }

  .mode-tab {
    flex: 1;
    padding: 8px;
    font-size: 0.82rem;
    font-weight: 800;
    border: none;
    border-radius: var(--radius-sm);
    background: transparent;
    cursor: pointer;
    color: var(--text-muted);
  }

  .mode-tab.active {
    background: var(--bg-surface);
    color: var(--accent-gold);
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  }

  .tool-pane {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .form-grid {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
  }

  .form-field {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .form-field label {
    font-size: 0.76rem;
    font-weight: 800;
    color: var(--text-muted);
  }

  .input-field {
    padding: 8px 12px;
    font-size: 0.9rem;
  }

  .calc-btn {
    padding: 10px;
    font-size: 0.9rem;
  }

  .result-card {
    background: var(--accent-gold-light);
    border: 1px solid var(--accent-gold);
    padding: 16px;
    border-radius: var(--radius-md);
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .result-amount {
    font-size: 1.35rem;
    font-weight: 900;
    color: var(--accent-gold);
  }

  .items-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-height: 220px;
    overflow-y: auto;
  }

  .receipt-item-card {
    background: var(--bg-surface-secondary);
    padding: 10px 14px;
    border-radius: var(--radius-md);
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .item-meta {
    display: flex;
    justify-content: space-between;
    font-size: 0.88rem;
    font-weight: 800;
  }

  .consumers-row {
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
  }

  .consumer-chip {
    padding: 3px 8px;
    font-size: 0.72rem;
    font-weight: 700;
    border-radius: 9999px;
    border: 1px solid var(--border-subtle);
    background: var(--bg-surface);
    cursor: pointer;
  }

  .consumer-chip.selected {
    background: var(--accent-gold);
    color: #ffffff;
    border-color: var(--accent-gold);
  }

  .summary-breakdown {
    background: var(--bg-surface);
    border: 1px solid var(--border-subtle);
    padding: 12px 14px;
    border-radius: var(--radius-md);
  }

  .summary-breakdown h4 {
    font-size: 0.82rem;
    font-weight: 800;
    margin-bottom: 8px;
  }

  .shares-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  .share-box {
    display: flex;
    flex-direction: column;
    align-items: center;
    background: var(--bg-surface-secondary);
    padding: 6px;
    border-radius: var(--radius-sm);
  }

  .person-name {
    font-size: 0.75rem;
    font-weight: 700;
    color: var(--text-muted);
  }

  .person-share {
    font-size: 0.9rem;
    font-weight: 900;
    color: var(--accent-gold);
  }

  .wheel-stage {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    margin-top: 10px;
  }

  .wheel-spinner {
    width: 100px;
    height: 100px;
    border-radius: 50%;
    border: 6px solid var(--accent-gold);
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 2.2rem;
    background: var(--bg-surface-secondary);
    transition: transform 1.5s cubic-bezier(0.15, 0.9, 0.35, 1);
  }

  .wheel-spinner.spinning {
    transform: rotate(1080deg);
  }

  .spin-act-btn {
    padding: 12px 28px;
    font-size: 0.95rem;
  }

  .winner-banner {
    background: rgba(200, 155, 60, 0.15);
    border: 2px solid var(--accent-gold);
    padding: 14px;
    border-radius: var(--radius-md);
    margin-top: 10px;
  }

  .winner-title {
    font-size: 0.72rem;
    font-weight: 900;
    color: var(--accent-gold);
    letter-spacing: 0.1em;
  }

  .winner-name {
    font-size: 1.25rem;
    font-weight: 900;
    color: var(--text-main);
  }

  .modal-footer {
    display: flex;
    justify-content: flex-end;
  }
</style>
