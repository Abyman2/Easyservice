import { writable } from 'svelte/store';

function loadSavedBookings() {
  try {
    const raw = localStorage.getItem('easyservice_bookings');
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export const userBookings = writable(loadSavedBookings());

userBookings.subscribe((val) => {
  try {
    localStorage.setItem('easyservice_bookings', JSON.stringify(val || []));
  } catch {
    /* ignore quota / private mode */
  }
});

export function addBooking(booking) {
  userBookings.update(items => [booking, ...items]);
}

export function updateBookingProviderStatus(bookingId, providerStatus) {
  userBookings.update(items => items.map(item => {
    if (item.id !== bookingId) return item;
    return {
      ...item,
      providerStatus,
      providerDecisionAt: new Date().toISOString(),
      providerDecisionMessage: providerStatus === 'ACCEPTED' ? 'Your provider accepted this booking.' : 'Your provider declined this booking.',
      status: providerStatus === 'DECLINED' ? 'DECLINED' : item.status
    };
  }));
}

export function cancelBookingItem(bookingId) {
  let refundAmount = 0;
  userBookings.update(items => {
    return items.map(item => {
      if (item.id === bookingId && item.status === 'CONFIRMED') {
        refundAmount = item.totalAmount;
        return { ...item, status: 'CANCELLED' };
      }
      return item;
    });
  });
  return refundAmount;
}
