  document.addEventListener('DOMContentLoaded', () => {
    const table = document.getElementById('request_items');
    const headers = table.querySelectorAll('thead th');
    let sortColumn = null;
    let sortDirection = 1;

    headers.forEach((header, columnIndex) => {
      header.style.cursor = 'pointer';
      header.classList.add('sortable'); // ← добавляем базовый класс с иконкой

      header.addEventListener('click', () => {
        const tbody = table.querySelector('tbody');
        const rows = Array.from(tbody.querySelectorAll('tr'));

        // Удаляем старые классы у всех заголовков
        headers.forEach(h => {
          h.classList.remove('asc', 'desc');
        });

        // Определяем направление сортировки
        if (sortColumn === columnIndex) {
          sortDirection *= -1;
        } else {
          sortColumn = columnIndex;
          sortDirection = 1;
        }

        // Добавляем текущий класс
        header.classList.add(sortDirection === 1 ? 'asc' : 'desc');

        // Сортировка строк
        rows.sort((rowA, rowB) => {
          const cellA = rowA.children[columnIndex].textContent.trim();
          const cellB = rowB.children[columnIndex].textContent.trim();

          const isDate = /^\d{2}\.\d{2}\.\d{4}$/.test(cellA) && /^\d{2}\.\d{2}\.\d{4}$/.test(cellB);
          if (isDate) {
            const [dA, mA, yA] = cellA.split('.').map(Number);
            const [dB, mB, yB] = cellB.split('.').map(Number);
            return sortDirection * (new Date(yA, mA - 1, dA) - new Date(yB, mB - 1, dB));
          }

          return sortDirection * cellA.localeCompare(cellB, 'ru');
        });

        // Перерисовка
        tbody.innerHTML = '';
        rows.forEach(row => tbody.appendChild(row));
      });
    });
  });