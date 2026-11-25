# Catzilla — product vision

Catzilla helps people look after their cats together. It tracks each cat's health, habits, weight and feeding times, and lets everyone who cares for the cats, including a temporary cat sitter, see the same information and record what they did.

## How it works

1. A person downloads the app and **registers** as a user.
2. They create a **Home**.
3. They share the Home's **invite link** with other people, such as a partner or flatmate, who join and manage the Home with them.
4. Inside the Home they register the **cats**, who are members of the family.
5. Each cat has a profile and a **feeding schedule**.
6. Later, a **cat sitter** can be given temporary access to look after the Home's cats.
7. When someone records a feeding or a note, the other people in the Home get a **notification**, for example *"Garfield ate 10 g of dry food"*.

## Concepts

| Concept | Meaning |
|---|---|
| **User** | Everybody who uses the app. In each Home they take part in, a user has a role: home member or cat sitter. |
| **Home** | A household and its cats. All cat data belongs to exactly one Home. |
| **Home member** | A user's role in a Home they manage. Joins by creating the Home or through its invite link. |
| **Cat sitter** | A user's role in a Home whose cats they look after temporarily, for a set period. Feeds the cats on schedule and writes notes. Planned for a later release. |
| **Cat** | A cat registered in a Home. |
| **Feeding schedule** | A cat's daily timeline of meals: time, amount and food. |
| **Feeding log** | A record that a meal was given: who, when and what. |
| **Diary note** | A note about a cat written by a member or sitter. Pictures come later. |
| **Notification** | A message to a Home's people about an event, such as a meal given or a note added. |

### Users and roles

Everybody who uses Catzilla is a **user**. "Home member" and "cat sitter" aren't kinds of users; they're the **role a user has in a particular Home**.

- A user can be a **home member of several Homes**, for example their own flat and a parent's house.
- A user can be a **cat sitter for one or several Homes**.
- The same user can hold **both roles at once in different Homes**: a member of their own Home while cat-sitting for friends.
- Within one Home, a user has **one role**: they're either a member or a sitter there.

Example:

| User | Home "Ana's flat" | Home "Bruno's house" | Home "Carla's place" |
|---|---|---|---|
| Ana | Home member | Cat sitter | — |
| Bruno | — | Home member | Cat sitter |
| Carla | Home member | — | Home member |

### Cat profile

- **Identity:** name, nickname, sex, and other regular characteristics (such as birth date or breed).
- **Health:** allergies, weight history.
- **Diet:** specific food, and amount of food per day.

### Feeding schedule example

| Time | Amount | Food |
|---|---|---|
| 07:00 | 10 g | Dry food |
| 09:00 | 20 g | Wet food |
| 12:00 | 10 g | Dry food with a spoon of water |

## Multi-tenancy: the Home is the boundary

Each **Home is a tenant**. Every cat, schedule, log, note and notification belongs to one Home, and a user only sees the Homes where they have a role. Because one user can have different roles in different Homes (see [Users and roles](#users-and-roles)), access is decided per Home and per role, never just per user:

- **Home members** have full access to the Home, including inviting others.
- **Cat sitters** can see the cats' health and diary, record feedings and add notes, and only during their access period.

Every API request that touches cat data has to check the user's role in that cat's Home. Since a user can be in several Homes, the app needs a way to switch between them. The technical rules are in [ADR-003](adr/ADR-003-multi-tenancy-users-and-roles.md).

## Releases

| Stage | Scope |
|---|---|
| **MVP** (web on a phone, home network) | Users, Homes, invite links, cats and profiles, feeding schedules, feeding logs |
| **Next** | Cat sitters with time-limited access, diary notes, notifications |
| **Later** | Picture upload, native iOS and Android apps |

## Open questions

- **Invite links:** do they expire, or work only once? Can a member be removed, and who can do it?
- **Time zones:** schedule times are probably in the Home's local time zone. Confirm before building schedules.
- **Units:** grams only, or should portions also allow units such as spoons or pouches?
- **Notifications:** which events notify whom, and through which channel (in-app, web push, native push)?
- **Missed feedings:** should a scheduled meal that isn't logged in time trigger a reminder?
- **Sitter access:** can a sitter edit a cat's profile or schedule, or only read it and log feedings and notes?
