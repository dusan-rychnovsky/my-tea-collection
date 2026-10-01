(function () {
  'use strict';

  var gallery = document.getElementById('tea-images');
  var modal = document.getElementById('imgModal');
  var modalImage = document.getElementById('modal-image');
  var previousButton = document.getElementById('gallery-previous');
  var nextButton = document.getElementById('gallery-next');
  var counter = document.getElementById('gallery-counter');

  if (!gallery || !modal || !modalImage || !previousButton || !nextButton || !counter) {
    return;
  }

  var images = Array.prototype.slice.call(gallery.querySelectorAll('.tea-gallery-image'));
  var currentIndex = 0;
  var activeTouchId = null;
  var touchStartX = null;
  var swipeThreshold = 50;

  function updateViewportHeight() {
    var viewportHeight = window.visualViewport ? window.visualViewport.height : window.innerHeight;
    modal.style.setProperty('--gallery-viewport-height', viewportHeight + 'px');
  }

  function showImage(index) {
    currentIndex = (index + images.length) % images.length;
    modalImage.src = images[currentIndex].src;
    modalImage.alt = images[currentIndex].alt;
    counter.textContent = (currentIndex + 1) + ' / ' + images.length;
  }

  function showPreviousImage() {
    showImage(currentIndex - 1);
  }

  function showNextImage() {
    showImage(currentIndex + 1);
  }

  if (images.length < 2) {
    previousButton.hidden = true;
    nextButton.hidden = true;
    counter.hidden = true;
  }

  updateViewportHeight();
  window.addEventListener('resize', updateViewportHeight);

  if (window.visualViewport) {
    window.visualViewport.addEventListener('resize', updateViewportHeight);
  }

  images.forEach(function (image, index) {
    image.addEventListener('click', function () {
      showImage(index);
    });
  });

  previousButton.addEventListener('click', showPreviousImage);
  nextButton.addEventListener('click', showNextImage);

  document.addEventListener('keydown', function (event) {
    if (!modal.classList.contains('show') || images.length < 2) {
      return;
    }

    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      showPreviousImage();
    } else if (event.key === 'ArrowRight') {
      event.preventDefault();
      showNextImage();
    }
  });

  modalImage.addEventListener('touchstart', function (event) {
    if (event.touches.length !== 1) {
      activeTouchId = null;
      touchStartX = null;
      return;
    }

    activeTouchId = event.touches[0].identifier;
    touchStartX = event.touches[0].clientX;
  }, { passive: true });

  modalImage.addEventListener('touchend', function (event) {
    var completedTouch = null;

    for (var i = 0; i < event.changedTouches.length; i++) {
      if (event.changedTouches[i].identifier === activeTouchId) {
        completedTouch = event.changedTouches[i];
        break;
      }
    }

    if (touchStartX === null || !completedTouch || images.length < 2) {
      activeTouchId = null;
      touchStartX = null;
      return;
    }

    var distance = completedTouch.clientX - touchStartX;
    activeTouchId = null;
    touchStartX = null;

    if (Math.abs(distance) < swipeThreshold) {
      return;
    }

    if (distance > 0) {
      showPreviousImage();
    } else {
      showNextImage();
    }
  }, { passive: true });

  modalImage.addEventListener('touchcancel', function () {
    activeTouchId = null;
    touchStartX = null;
  }, { passive: true });
}());
