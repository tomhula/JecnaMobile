# Ječná Mobile

Ječná Mobile is an Android app made for students of [SPŠE Ječná in Prague](https://www.spsejecna.cz/). It was developed to
make reading the school portal more convenient. The app is **UNOFFICIAL** and therefore **has no connection to the school’s
official software**.

You can participate in development: [Contributing](#contributing)

## Installation

[<img src="readme-res/get_it_on.png" alt="Get it on Google Play" height="50">](https://play.google.com/store/apps/details?id=me.tomasan7.jecnamobile)    
The minimum supported Android version is `Android 8.0 (Oreo)`.

## Features

- [x] Grades and average for each subject  
- [x] Timetable  
- [x] Ordering lunches from the school canteen  
- [x] Arrivals and departures  
- [x] News  
- [x] Teaching staff  
- [x] Notifications  
- [x] Grade predictor  
- [x] Canteen marketplace  

## Screenshots

<p float="left">
  <a href="readme-res/screenshots/grades.png">
    <img alt="grades" src="readme-res/screenshots/grades.png" width="200px" />
  </a>
  <a href="readme-res/screenshots/timetable.png">
    <img alt="timetable" src="readme-res/screenshots/timetable.png" width="200px" />
  </a>
  <a href="readme-res/screenshots/news.png">
    <img alt="news" src="readme-res/screenshots/news.png" width="200px" />
  </a>
  <a href="readme-res/screenshots/canteen.png">
    <img alt="canteen" src="readme-res/screenshots/canteen.png" width="200px" />
  </a>
  <a href="readme-res/screenshots/attendances.png">
    <img alt="arrivals and departures" src="readme-res/screenshots/attendances.png" width="200px" />
  </a>
  <a href="readme-res/screenshots/teachers.png">
    <img alt="teachers" src="readme-res/screenshots/teachers.png" width="200px" />
  </a>
</p>

## Known Issues

- When the network state changes while the app is minimized, a toast saying “An error occurred during login” is shown
- Several parts of the app do not work if the user does not check “Remember user”
- Clicking on a notification only opens the app but does not navigate to a specific screen

## Contributing

Every Ječná student with an Android device will appreciate your contribution, so feel free to get involved.

I should warn you in advance that the app’s code quality is quite poor and you should not use it as inspiration. This is due to the relatively high complexity of Android development and limited time.

### Info

- Ječná Mobile is based on the [JecnaAPI](https://github.com/tomhula/JecnaAPI) library, so if you want to add a new feature related with the website, you will also need to modify JecnaAPI  
- The entire app is written in [Kotlin](https://kotlinlang.org/docs/getting-started.html)

### Setup

1. Install [Android Studio](https://developer.android.com/studio).
2. Fork this project.
3. Clone your fork, either via terminal or using `Android Studio > New Project from VCS`.
4. Wait for everything to load.
5. From here on, you either know what to do or follow some tutorials :)
6. For testing, I recommend using a physical device instead of an emulator. It’s much faster and it can also be connected via Wi-Fi, so you don’t need to keep it plugged in with a cable.

### Rules

- Write commit messages in English and in the imperative mood. Use meaningful descriptions. Look at existing commits for reference.
- It would be nice to keep formatting consistent with the rest of the project, but it’s not mandatory.
- Always test the app before opening a pull request.
- If you plan to make larger changes, try to consult them with me first.

Even though my code is ugly, yours doesn’t have to be :)
Feel free to clean up or improve the existing code as well.

Once you add your code, create a pull request and write a meaningful description.

## License

[GNU GPLv3](LICENSE) © Tomáš Hůla
