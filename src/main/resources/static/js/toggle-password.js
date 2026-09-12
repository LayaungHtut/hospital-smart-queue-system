function togglePassword(inputId) {
    var input = document.getElementById(inputId);
    var btn = input.nextElementSibling;
    var icon = btn.querySelector('.material-symbols-outlined');
    if (input.type === "password") {
        input.type = "text";
        if (icon) icon.textContent = 'visibility';
    } else {
        input.type = "password";
        if (icon) icon.textContent = 'visibility_off';
    }
}
