import SwiftUI

@main
struct RostsvarApp: App {
    var body: some Scene {
        WindowGroup {
            // Dikteringsskärmen är startskärmen, som på Android. Guiden och
            // Om ligger som knappar högst upp på den.
            HomeScreen()
        }
    }
}
