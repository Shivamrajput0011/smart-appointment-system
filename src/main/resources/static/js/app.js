'use strict';

/* Smart Appointment Management System - frontend logic (plain JavaScript, no libraries). */

const API = '/api';
const state = { pets: [], allPets: [], appointments: [] };

/* ------------------------------------------------------------------ helpers */

const $ = (selector) => document.querySelector(selector);

/** Escapes text before putting it into HTML, so user input can never inject markup. */
function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function todayISO() {
    const now = new Date();
    return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
}

function shortTime(time) {
    return time ? String(time).slice(0, 5) : '';
}

function statusLabel(status) {
    return status.charAt(0) + status.slice(1).toLowerCase();
}

/** Calls the REST API. Throws an Error whose message is safe to show to the user. */
async function request(path, options = {}) {
    let response;
    try {
        response = await fetch(API + path, {
            headers: { 'Content-Type': 'application/json' },
            ...options,
        });
    } catch (networkError) {
        throw new Error('Cannot reach the server. Check that the application is running.');
    }
    if (response.status === 204) {
        return null;
    }
    let body = null;
    try {
        body = await response.json();
    } catch (ignored) {
        // body stays null when the response is not JSON
    }
    if (!response.ok) {
        const fieldMessages = body && body.fieldErrors ? Object.values(body.fieldErrors) : [];
        const message = fieldMessages.length
            ? fieldMessages.join(' ')
            : (body && body.message) || `Request failed (HTTP ${response.status}).`;
        const error = new Error(message);
        error.fieldErrors = body ? body.fieldErrors : null;
        throw error;
    }
    return body;
}

function toast(message, isError = false) {
    const node = document.createElement('div');
    node.className = 'toast' + (isError ? ' error' : '');
    node.textContent = message;
    $('#toast-region').appendChild(node);
    setTimeout(() => node.remove(), isError ? 7000 : 4000);
}

function showFormError(selector, message) {
    const node = $(selector);
    node.textContent = message;
    node.hidden = !message;
}

function markInvalid(form, fieldErrors) {
    form.querySelectorAll('.invalid').forEach((el) => el.classList.remove('invalid'));
    if (!fieldErrors) return;
    const map = {
        name: '#pet-name', type: '#pet-type', breed: '#pet-breed', age: '#pet-age',
        ownerName: '#owner-name', ownerPhone: '#owner-phone', ownerEmail: '#owner-email',
        petId: '#appt-pet', appointmentDate: '#appt-date', appointmentTime: '#appt-time', reason: '#appt-reason',
    };
    Object.keys(fieldErrors).forEach((field) => {
        const input = map[field] && form.querySelector(map[field]);
        if (input) input.classList.add('invalid');
    });
}

/* --------------------------------------------------------------- navigation */

function showView(name) {
    document.querySelectorAll('.view').forEach((v) => { v.hidden = v.id !== 'view-' + name; });
    document.querySelectorAll('.nav-btn').forEach((b) =>
        b.setAttribute('aria-current', b.dataset.view === name ? 'page' : 'false'));
    history.replaceState(null, '', '#' + name);

    if (name === 'dashboard') loadDashboard();
    if (name === 'pets') loadPets();
    if (name === 'appointments') { refreshPetOptions().then(loadAppointments); }
}

/* ---------------------------------------------------------------- dashboard */

async function loadDashboard() {
    try {
        const d = await request('/dashboard');
        $('#stat-upcoming').textContent = d.upcomingAppointments;
        $('#stat-pets').textContent = d.totalPets;
        $('#stat-total').textContent = d.totalAppointments;
        $('#stat-completed').textContent = d.completedAppointments;
        $('#stat-cancelled').textContent = d.cancelledAppointments;

        $('#upcoming-body').innerHTML = d.upcomingList.length
            ? d.upcomingList.map((a) => `
                <tr>
                    <td>${esc(a.appointmentDate)}</td>
                    <td>${esc(shortTime(a.appointmentTime))}</td>
                    <td>${esc(a.petName)} <span class="hint">#${esc(a.petId)}</span></td>
                    <td>${esc(a.ownerName)}</td>
                    <td>${esc(a.reason)}</td>
                </tr>`).join('')
            : '<tr><td class="empty" colspan="5">No upcoming appointments. Schedule one from the Appointments section.</td></tr>';
    } catch (e) {
        toast(e.message, true);
    }
}

/* --------------------------------------------------------------------- pets */

function renderPets() {
    $('#pets-body').innerHTML = state.pets.length
        ? state.pets.map((p) => `
            <tr>
                <td>${esc(p.id)}</td>
                <td>${esc(p.name)}</td>
                <td>${esc(p.type)}</td>
                <td>${esc(p.breed || '-')}</td>
                <td>${esc(p.age)}</td>
                <td>${esc(p.ownerName)}</td>
                <td>${esc(p.ownerPhone)}${p.ownerEmail ? '<br><span class="hint">' + esc(p.ownerEmail) + '</span>' : ''}</td>
                <td class="row-actions">
                    <button type="button" class="btn btn-small" data-action="edit-pet" data-id="${esc(p.id)}">Edit</button>
                    <button type="button" class="btn btn-small btn-danger" data-action="delete-pet" data-id="${esc(p.id)}">Delete</button>
                </td>
            </tr>`).join('')
        : '<tr><td class="empty" colspan="8">No pets found. Register a pet using the form above.</td></tr>';
}

async function loadPets() {
    const term = $('#pet-search').value.trim();
    try {
        state.pets = await request('/pets' + (term ? '?search=' + encodeURIComponent(term) : ''));
        renderPets();
    } catch (e) {
        toast(e.message, true);
    }
}

function resetPetForm() {
    $('#pet-form').reset();
    $('#pet-id').value = '';
    $('#pet-form-title').textContent = 'Register a pet';
    $('#pet-submit').textContent = 'Register pet';
    $('#pet-cancel-edit').hidden = true;
    showFormError('#pet-form-error', '');
    markInvalid($('#pet-form'), null);
}

function startEditPet(id) {
    const pet = state.pets.find((p) => String(p.id) === String(id));
    if (!pet) return;
    $('#pet-id').value = pet.id;
    $('#pet-name').value = pet.name;
    $('#pet-type').value = pet.type;
    $('#pet-breed').value = pet.breed || '';
    $('#pet-age').value = pet.age;
    $('#owner-name').value = pet.ownerName;
    $('#owner-phone').value = pet.ownerPhone;
    $('#owner-email').value = pet.ownerEmail || '';
    $('#pet-form-title').textContent = 'Update pet #' + pet.id;
    $('#pet-submit').textContent = 'Save changes';
    $('#pet-cancel-edit').hidden = false;
    showFormError('#pet-form-error', '');
    $('#pet-form').scrollIntoView({ behavior: 'smooth', block: 'start' });
    $('#pet-name').focus();
}

async function submitPet(event) {
    event.preventDefault();
    const form = $('#pet-form');
    const id = $('#pet-id').value;
    const ageText = $('#pet-age').value;
    const payload = {
        name: $('#pet-name').value.trim(),
        type: $('#pet-type').value.trim(),
        breed: $('#pet-breed').value.trim() || null,
        age: ageText === '' ? null : Number(ageText),
        ownerName: $('#owner-name').value.trim(),
        ownerPhone: $('#owner-phone').value.trim(),
        ownerEmail: $('#owner-email').value.trim() || null,
    };
    try {
        await request(id ? '/pets/' + id : '/pets', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(payload),
        });
        toast(id ? 'Pet details updated.' : 'Pet registered.');
        resetPetForm();
        $('#pet-search').value = '';
        await loadPets();
    } catch (e) {
        markInvalid(form, e.fieldErrors);
        showFormError('#pet-form-error', e.message);
        toast(e.message, true);
    }
}

async function deletePet(id) {
    const pet = state.pets.find((p) => String(p.id) === String(id));
    const name = pet ? pet.name : 'this pet';
    if (!confirm(`Delete ${name}? All of this pet's appointments will be deleted too.`)) return;
    try {
        await request('/pets/' + id, { method: 'DELETE' });
        toast(`${name} was deleted.`);
        await loadPets();
    } catch (e) {
        toast(e.message, true);
    }
}

/* ------------------------------------------------------------- appointments */

async function refreshPetOptions() {
    try {
        state.allPets = await request('/pets');
        const select = $('#appt-pet');
        const current = select.value;
        select.innerHTML = state.allPets.length
            ? '<option value="">Choose a registered pet</option>' + state.allPets.map((p) =>
                `<option value="${esc(p.id)}">#${esc(p.id)} ${esc(p.name)} (owner: ${esc(p.ownerName)})</option>`).join('')
            : '<option value="">No pets registered yet - register one first</option>';
        select.value = current;
    } catch (e) {
        toast(e.message, true);
    }
}

function renderAppointments() {
    $('#appts-body').innerHTML = state.appointments.length
        ? state.appointments.map((a) => {
            const scheduled = a.status === 'SCHEDULED';
            return `
            <tr>
                <td>${esc(a.id)}</td>
                <td>${esc(a.petName)} <span class="hint">#${esc(a.petId)}</span></td>
                <td>${esc(a.ownerName)}</td>
                <td>${esc(a.appointmentDate)}</td>
                <td>${esc(shortTime(a.appointmentTime))}</td>
                <td>${esc(a.reason)}</td>
                <td><span class="badge badge-${esc(a.status)}">${esc(statusLabel(a.status))}</span></td>
                <td class="row-actions">
                    ${scheduled ? `
                    <button type="button" class="btn btn-small" data-action="edit-appt" data-id="${esc(a.id)}">Edit</button>
                    <button type="button" class="btn btn-small" data-action="complete-appt" data-id="${esc(a.id)}">Complete</button>
                    <button type="button" class="btn btn-small btn-danger" data-action="cancel-appt" data-id="${esc(a.id)}">Cancel</button>` : ''}
                    <button type="button" class="btn btn-small btn-quiet" data-action="delete-appt" data-id="${esc(a.id)}">Delete</button>
                </td>
            </tr>`;
        }).join('')
        : '<tr><td class="empty" colspan="8">No appointments found.</td></tr>';
}

async function loadAppointments() {
    const params = new URLSearchParams();
    const id = $('#appt-search-id').value.trim();
    const date = $('#appt-search-date').value;
    if (id) params.set('id', id);
    if (date) params.set('date', date);
    const query = params.toString();
    try {
        state.appointments = await request('/appointments' + (query ? '?' + query : ''));
        renderAppointments();
    } catch (e) {
        toast(e.message, true);
    }
}

function resetApptForm() {
    $('#appt-form').reset();
    $('#appt-id').value = '';
    $('#appt-form-title').textContent = 'Schedule an appointment';
    $('#appt-submit').textContent = 'Schedule appointment';
    $('#appt-cancel-edit').hidden = true;
    $('#appt-status-group').hidden = true;
    $('#appt-date').min = todayISO();
    showFormError('#appt-form-error', '');
    markInvalid($('#appt-form'), null);
}

function startEditAppt(id) {
    const a = state.appointments.find((x) => String(x.id) === String(id));
    if (!a) return;
    $('#appt-id').value = a.id;
    $('#appt-pet').value = a.petId;
    $('#appt-date').removeAttribute('min'); // allow keeping an existing date
    $('#appt-date').value = a.appointmentDate;
    $('#appt-time').value = shortTime(a.appointmentTime);
    $('#appt-reason').value = a.reason;
    $('#appt-status').value = a.status;
    $('#appt-status-group').hidden = false;
    $('#appt-form-title').textContent = 'Update appointment #' + a.id;
    $('#appt-submit').textContent = 'Save changes';
    $('#appt-cancel-edit').hidden = false;
    showFormError('#appt-form-error', '');
    $('#appt-form').scrollIntoView({ behavior: 'smooth', block: 'start' });
    $('#appt-reason').focus();
}

async function submitAppt(event) {
    event.preventDefault();
    const form = $('#appt-form');
    const id = $('#appt-id').value;
    const petValue = $('#appt-pet').value;
    const payload = {
        petId: petValue === '' ? null : Number(petValue),
        appointmentDate: $('#appt-date').value || null,
        appointmentTime: $('#appt-time').value || null,
        reason: $('#appt-reason').value.trim(),
        status: id ? $('#appt-status').value : null,
    };
    try {
        await request(id ? '/appointments/' + id : '/appointments', {
            method: id ? 'PUT' : 'POST',
            body: JSON.stringify(payload),
        });
        toast(id ? 'Appointment updated.' : 'Appointment scheduled.');
        resetApptForm();
        await loadAppointments();
    } catch (e) {
        markInvalid(form, e.fieldErrors);
        showFormError('#appt-form-error', e.message);
        toast(e.message, true);
    }
}

async function changeApptStatus(id, action, doneMessage, confirmText) {
    if (confirmText && !confirm(confirmText)) return;
    try {
        await request(`/appointments/${id}/${action}`, { method: 'PATCH' });
        toast(doneMessage);
        await loadAppointments();
    } catch (e) {
        toast(e.message, true);
    }
}

async function deleteAppt(id) {
    if (!confirm(`Permanently delete appointment #${id}?`)) return;
    try {
        await request('/appointments/' + id, { method: 'DELETE' });
        toast('Appointment deleted.');
        await loadAppointments();
    } catch (e) {
        toast(e.message, true);
    }
}

/* -------------------------------------------------------------------- start */

function init() {
    document.querySelectorAll('.nav-btn').forEach((b) =>
        b.addEventListener('click', () => showView(b.dataset.view)));

    // Pets
    $('#pet-form').addEventListener('submit', submitPet);
    $('#pet-cancel-edit').addEventListener('click', resetPetForm);
    $('#pet-search-form').addEventListener('submit', (e) => { e.preventDefault(); loadPets(); });
    $('#pet-search-clear').addEventListener('click', () => { $('#pet-search').value = ''; loadPets(); });
    $('#pets-body').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        if (btn.dataset.action === 'edit-pet') startEditPet(btn.dataset.id);
        if (btn.dataset.action === 'delete-pet') deletePet(btn.dataset.id);
    });

    // Appointments
    $('#appt-form').addEventListener('submit', submitAppt);
    $('#appt-cancel-edit').addEventListener('click', resetApptForm);
    $('#appt-search-form').addEventListener('submit', (e) => { e.preventDefault(); loadAppointments(); });
    $('#appt-search-clear').addEventListener('click', () => {
        $('#appt-search-id').value = '';
        $('#appt-search-date').value = '';
        loadAppointments();
    });
    $('#appts-body').addEventListener('click', (e) => {
        const btn = e.target.closest('button[data-action]');
        if (!btn) return;
        const id = btn.dataset.id;
        switch (btn.dataset.action) {
            case 'edit-appt': startEditAppt(id); break;
            case 'complete-appt': changeApptStatus(id, 'complete', 'Appointment marked as completed.'); break;
            case 'cancel-appt': changeApptStatus(id, 'cancel', 'Appointment cancelled.',
                `Cancel appointment #${id}?`); break;
            case 'delete-appt': deleteAppt(id); break;
            default: break;
        }
    });

    resetApptForm();
    const initial = location.hash.replace('#', '');
    showView(['dashboard', 'pets', 'appointments'].includes(initial) ? initial : 'dashboard');
}

document.addEventListener('DOMContentLoaded', init);
